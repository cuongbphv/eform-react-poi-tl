import React, { useState, useEffect } from 'react';
import { Upload, FileText, Plus, Download, Edit, Save, X, FileEdit } from 'lucide-react';
import OnlyOfficeEditor from './OnlyOfficeEditor';

const API_BASE_URL = 'http://192.168.100.244:8080/api/v1';

const App = () => {
    const [activeTab, setActiveTab] = useState('templates');
    const [templates, setTemplates] = useState([]);
    const [forms, setForms] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [selectedTemplate, setSelectedTemplate] = useState(null);
    const [formData, setFormData] = useState({});
    const [formName, setFormName] = useState('');
    const [showFormModal, setShowFormModal] = useState(false);
    const [editingForm, setEditingForm] = useState(null);

    // OnlyOffice states
    const [showOnlyOfficeEditor, setShowOnlyOfficeEditor] = useState(false);
    const [editingTemplateId, setEditingTemplateId] = useState(null);

    // Load templates and forms on component mount
    useEffect(() => {
        loadTemplates();
        loadForms();
        testOnlyOfficeConnection();
    }, []);

    const testOnlyOfficeConnection = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/onlyoffice/test`);
            const result = await response.json();
            console.log('OnlyOffice connection test:', result);
        } catch (err) {
            console.warn('OnlyOffice connection test failed:', err);
        }
    };

    const loadTemplates = async () => {
        try {
            setLoading(true);
            const response = await fetch(`${API_BASE_URL}/templates`);
            if (response.ok) {
                const data = await response.json();
                setTemplates(data);
            } else {
                setError('Không thể tải danh sách template');
            }
        } catch (err) {
            setError('Lỗi kết nối: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const loadForms = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/forms`);
            if (response.ok) {
                const data = await response.json();
                setForms(data);
            } else {
                setError('Không thể tải danh sách form');
            }
        } catch (err) {
            setError('Lỗi kết nối: ' + err.message);
        }
    };

    const handleFileUpload = async (event) => {
        const file = event.target.files[0];
        if (!file) return;

        if (!file.name.toLowerCase().endsWith('.docx')) {
            setError('Vui lòng chọn file Word (.docx)');
            return;
        }

        const templateName = prompt('Nhập tên template:');
        if (!templateName) return;

        const formData = new FormData();
        formData.append('file', file);
        formData.append('name', templateName);

        try {
            setLoading(true);
            const response = await fetch(`${API_BASE_URL}/templates/upload`, {
                method: 'POST',
                body: formData,
            });

            if (response.ok) {
                const newTemplate = await response.json();
                setTemplates([...templates, newTemplate]);
                setError('');
                alert('Upload template thành công!');
            } else {
                setError('Không thể upload template');
            }
        } catch (err) {
            setError('Lỗi upload: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleCreateForm = (template) => {
        setSelectedTemplate(template);
        setFormData({});
        setFormName('');
        setShowFormModal(true);
        setEditingForm(null);
    };

    const handleEditForm = (form) => {
        setSelectedTemplate(templates.find(t => t.id === form.templateId));
        setFormData(form.formData);
        setFormName(form.name);
        setShowFormModal(true);
        setEditingForm(form);
    };

    // OnlyOffice functions
    const handleEditTemplate = (templateId) => {
        setEditingTemplateId(templateId);
        setShowOnlyOfficeEditor(true);
    };

    const handleCloseOnlyOffice = () => {
        setShowOnlyOfficeEditor(false);
        setEditingTemplateId(null);
        // Reload templates để cập nhật changes
        setTimeout(() => {
            loadTemplates();
        }, 1000);
    };

    const handleSaveFromOnlyOffice = () => {
        console.log('Template saved from OnlyOffice');
        // OnlyOffice sẽ tự động save qua callback
    };

    const handleSaveForm = async () => {
        if (!formName.trim()) {
            setError('Vui lòng nhập tên form');
            return;
        }

        try {
            setLoading(true);
            const requestData = {
                templateId: selectedTemplate.id,
                name: formName,
                data: formData,
            };

            const response = await fetch(`${API_BASE_URL}/forms`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestData),
            });

            if (response.ok) {
                const savedForm = await response.json();
                if (editingForm) {
                    setForms(forms.map(f => f.id === editingForm.id ? savedForm : f));
                } else {
                    setForms([...forms, savedForm]);
                }
                setShowFormModal(false);
                setError('');
                alert('Lưu form thành công!');
            } else {
                setError('Không thể lưu form');
            }
        } catch (err) {
            setError('Lỗi lưu form: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleGenerateEnhancedPdf = async (form) => {
        try {
            setLoading(true);
            setError('');

            const response = await fetch(`${API_BASE_URL}/forms/${form.id}/generate-pdf-enhanced`, {
                method: 'POST',
            });

            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `enhanced_${form.name}.pdf`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                setError('Tạo PDF cải thiện thành công!');
                setTimeout(() => setError(''), 3000);
            } else {
                console.warn('Enhanced PDF failed, trying standard PDF...');
                await handleGeneratePdf(form);
            }
        } catch (err) {
            console.error('Enhanced PDF error:', err);
            setError('⚠PDF cải thiện lỗi, đang thử phương pháp chuẩn...');

            try {
                await handleGeneratePdf(form);
            } catch (fallbackErr) {
                setError('Không thể tạo PDF: ' + fallbackErr.message);
            }
        } finally {
            setLoading(false);
        }
    };

    const handleGeneratePdf = async (form) => {
        try {
            setLoading(true);
            const response = await fetch(`${API_BASE_URL}/forms/${form.id}/generate-pdf`, {
                method: 'POST',
            });

            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `${form.name}.pdf`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                setError('');
            } else {
                setError('Không thể tạo PDF');
            }
        } catch (err) {
            setError('Lỗi tạo PDF: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (variableName, value) => {
        let cleanValue = value;
        if (typeof value === 'string') {
            cleanValue = value
                .replace(/\r\n/g, ' ')
                .replace(/\n/g, ' ')
                .replace(/\r/g, ' ')
                .replace(/\s+/g, ' ')
                .trim();
        }

        setFormData(prev => ({
            ...prev,
            [variableName]: cleanValue
        }));
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleString('vi-VN');
    };

    // Render OnlyOffice Editor
    if (showOnlyOfficeEditor && editingTemplateId) {
        return (
            <OnlyOfficeEditor
                templateId={editingTemplateId}
                onClose={handleCloseOnlyOffice}
                onSave={handleSaveFromOnlyOffice}
            />
        );
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <div className="max-w-7xl mx-auto py-8 px-4">
                <h1 className="text-3xl font-bold text-gray-900 mb-8">
                    Hệ thống E-Form với OnlyOffice Integration
                </h1>

                {error && (
                    <div className={`mb-4 p-4 border rounded ${
                        error.includes('✅') ? 'bg-green-100 border-green-400 text-green-700' :
                            error.includes('⚠️') ? 'bg-yellow-100 border-yellow-400 text-yellow-700' :
                                'bg-red-100 border-red-400 text-red-700'
                    }`}>
                        {error}
                    </div>
                )}

                <div className="flex mb-6 border-b border-gray-200">
                    <button
                        onClick={() => setActiveTab('templates')}
                        className={`px-6 py-3 font-medium text-sm border-b-2 ${
                            activeTab === 'templates'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700'
                        }`}
                    >
                        <FileText className="inline w-4 h-4 mr-2" />
                        Templates
                    </button>
                    <button
                        onClick={() => setActiveTab('forms')}
                        className={`px-6 py-3 font-medium text-sm border-b-2 ${
                            activeTab === 'forms'
                                ? 'border-blue-500 text-blue-600'
                                : 'border-transparent text-gray-500 hover:text-gray-700'
                        }`}
                    >
                        <Edit className="inline w-4 h-4 mr-2" />
                        Forms
                    </button>
                </div>

                {activeTab === 'templates' && (
                    <div>
                        <div className="mb-6">
                            <label className="flex items-center justify-center w-full h-32 border-2 border-gray-300 border-dashed rounded-lg cursor-pointer bg-gray-50 hover:bg-gray-100">
                                <div className="flex flex-col items-center justify-center pt-5 pb-6">
                                    <Upload className="w-8 h-8 mb-4 text-gray-500" />
                                    <p className="mb-2 text-sm text-gray-500">
                                        <span className="font-semibold">Click để upload</span> hoặc kéo thả file
                                    </p>
                                    <p className="text-xs text-gray-500">Word (.docx)</p>
                                </div>
                                <input
                                    type="file"
                                    className="hidden"
                                    accept=".docx"
                                    onChange={handleFileUpload}
                                />
                            </label>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {templates.map((template) => (
                                <div key={template.id} className="bg-white rounded-lg shadow-md p-6">
                                    <div className="flex items-center justify-between mb-4">
                                        <h3 className="text-lg font-semibold text-gray-900">{template.name}</h3>
                                        <FileText className="w-6 h-6 text-gray-400" />
                                    </div>
                                    <p className="text-sm text-gray-600 mb-2">File: {template.filename}</p>
                                    <p className="text-sm text-gray-600 mb-4">
                                        Biến: {template.variables.length} biến
                                    </p>
                                    <div className="mb-4">
                                        <p className="text-xs text-gray-500 mb-2">Danh sách biến:</p>
                                        <div className="flex flex-wrap gap-1">
                                            {template.variables.map((variable, index) => (
                                                <span
                                                    key={index}
                                                    className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded"
                                                >
                          {variable}
                        </span>
                                            ))}
                                        </div>
                                    </div>
                                    <p className="text-xs text-gray-500 mb-4">
                                        Tạo: {formatDate(template.createdAt)}
                                    </p>

                                    {/* Template Actions */}
                                    <div className="space-y-2">
                                        <button
                                            onClick={() => handleEditTemplate(template.id)}
                                            className="w-full bg-purple-600 text-white py-2 px-4 rounded hover:bg-purple-700 transition-colors"
                                            title="Chỉnh sửa template với OnlyOffice"
                                        >
                                            <FileEdit className="inline w-4 h-4 mr-2" />
                                            Edit với OnlyOffice
                                        </button>

                                        <button
                                            onClick={() => handleCreateForm(template)}
                                            className="w-full bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700 transition-colors"
                                        >
                                            <Plus className="inline w-4 h-4 mr-2" />
                                            Tạo Form
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {activeTab === 'forms' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {forms.map((form) => (
                            <div key={form.id} className="bg-white rounded-lg shadow-md p-6">
                                <div className="flex items-center justify-between mb-4">
                                    <h3 className="text-lg font-semibold text-gray-900">{form.name}</h3>
                                    <Edit className="w-6 h-6 text-gray-400" />
                                </div>
                                <p className="text-sm text-gray-600 mb-2">Template: {form.templateName}</p>
                                <p className="text-xs text-gray-500 mb-4">
                                    Cập nhật: {formatDate(form.updatedAt)}
                                </p>

                                <div className="space-y-2">
                                    <button
                                        onClick={() => handleEditForm(form)}
                                        className="w-full bg-yellow-600 text-white py-2 px-4 rounded hover:bg-yellow-700 transition-colors text-sm"
                                    >
                                        <Edit className="inline w-4 h-4 mr-2" />
                                        Sửa Form
                                    </button>

                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => handleGenerateEnhancedPdf(form)}
                                            className="flex-1 bg-green-600 text-white py-2 px-3 rounded hover:bg-green-700 transition-colors text-sm"
                                            title="PDF với format cải thiện"
                                        >
                                            <Download className="inline w-4 h-4 mr-1" />
                                            PDF Pro
                                        </button>
                                        <button
                                            onClick={() => handleGeneratePdf(form)}
                                            className="flex-1 bg-blue-600 text-white py-2 px-3 rounded hover:bg-blue-700 transition-colors text-sm"
                                            title="PDF tiêu chuẩn"
                                        >
                                            <FileText className="inline w-4 h-4 mr-1" />
                                            PDF
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {showFormModal && selectedTemplate && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                        <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                            <div className="p-6 border-b border-gray-200">
                                <div className="flex items-center justify-between">
                                    <h2 className="text-xl font-semibold text-gray-900">
                                        {editingForm ? 'Chỉnh sửa Form' : 'Tạo Form Mới'}
                                    </h2>
                                    <button
                                        onClick={() => setShowFormModal(false)}
                                        className="text-gray-400 hover:text-gray-600"
                                    >
                                        <X className="w-6 h-6" />
                                    </button>
                                </div>
                                <p className="text-sm text-gray-600 mt-2">Template: {selectedTemplate.name}</p>
                            </div>

                            <div className="p-6">
                                <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                                    <p className="text-sm text-blue-700">
                                        💡 <strong>OnlyOffice Integration:</strong> Template có thể được chỉnh sửa trực tiếp với OnlyOffice Editor
                                    </p>
                                </div>

                                <div className="mb-6">
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Tên Form
                                    </label>
                                    <input
                                        type="text"
                                        value={formName}
                                        onChange={(e) => setFormName(e.target.value)}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        placeholder="Nhập tên form"
                                    />
                                </div>

                                <div className="space-y-4">
                                    <h3 className="text-lg font-medium text-gray-900">Điền thông tin</h3>
                                    {selectedTemplate.variables.map((variable) => {
                                        const isMoneyField = variable.toLowerCase().includes('tien') ||
                                            variable.toLowerCase().includes('gia') ||
                                            variable.toLowerCase().includes('phi');
                                        const isDateField = variable.toLowerCase().includes('ngay') ||
                                            variable.toLowerCase().includes('date');

                                        return (
                                            <div key={variable}>
                                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                                    {variable}
                                                    {isMoneyField && <span className="text-blue-500 ml-1">(VND)</span>}
                                                    {isDateField && <span className="text-green-500 ml-1">(dd/mm/yyyy)</span>}
                                                </label>
                                                <input
                                                    type={isDateField ? "date" : "text"}
                                                    value={formData[variable] || ''}
                                                    onChange={(e) => handleInputChange(variable, e.target.value)}
                                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                                    placeholder={
                                                        isMoneyField ? `Nhập số tiền cho ${variable}` :
                                                            isDateField ? `Chọn ngày cho ${variable}` :
                                                                `Nhập giá trị cho ${variable}`
                                                    }
                                                />
                                                {formData[variable] && (
                                                    <p className="text-xs text-gray-500 mt-1">
                                                        Đã làm sạch: {formData[variable]}
                                                    </p>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>

                            <div className="p-6 border-t border-gray-200 flex gap-4">
                                <button
                                    onClick={handleSaveForm}
                                    disabled={loading}
                                    className="flex-1 bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700 transition-colors disabled:opacity-50"
                                >
                                    <Save className="inline w-4 h-4 mr-2" />
                                    {loading ? 'Đang lưu...' : 'Lưu Form'}
                                </button>
                                <button
                                    onClick={() => setShowFormModal(false)}
                                    className="flex-1 bg-gray-600 text-white py-2 px-4 rounded hover:bg-gray-700 transition-colors"
                                >
                                    Hủy
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {loading && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-white p-6 rounded-lg">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                            <p className="mt-4 text-gray-600">Đang xử lý...</p>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default App;