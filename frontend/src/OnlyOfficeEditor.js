import React, { useEffect, useRef, useState } from 'react';
import { Save, X, Download, Eye } from 'lucide-react';

const API_BASE_URL = 'http://192.168.100.244:8080/api/v1';

const OnlyOfficeEditor = ({ templateId, onClose, onSave }) => {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [editorInstance, setEditorInstance] = useState(null);
    const editorRef = useRef(null);

    useEffect(() => {
        if (templateId) {
            loadEditor();
        }

        // Cleanup khi component unmount
        return () => {
            if (editorInstance) {
                try {
                    editorInstance.destroyEditor();
                } catch (e) {
                    console.warn('Error destroying editor:', e);
                }
            }
        };
    }, [templateId]);

    const loadEditor = async () => {
        try {
            setLoading(true);
            setError('');

            // Load OnlyOffice API script nếu chưa có
            if (!window.DocsAPI) {
                await loadOnlyOfficeScript();
            }

            // Get editor config từ backend
            const response = await fetch(`${API_BASE_URL}/onlyoffice/config/${templateId}?userId=user1&userName=Editor`);

            if (!response.ok) {
                throw new Error('Failed to get editor config');
            }

            const config = await response.json();

            // Initialize OnlyOffice editor
            initializeEditor(config);

        } catch (err) {
            console.error('Error loading editor:', err);
            setError('Không thể tải editor: ' + err.message);
            setLoading(false);
        }
    };

    const loadOnlyOfficeScript = () => {
        return new Promise((resolve, reject) => {
            // Remove existing script nếu có
            const existingScript = document.getElementById('onlyoffice-api');
            if (existingScript) {
                existingScript.remove();
            }

            const script = document.createElement('script');
            script.id = 'onlyoffice-api';
            script.src = 'http://192.168.100.244:8081/web-apps/apps/api/documents/api.js'; // OnlyOffice Document Server URL
            script.async = true;

            script.onload = () => {
                console.log('OnlyOffice API script loaded');
                resolve();
            };

            script.onerror = () => {
                reject(new Error('Failed to load OnlyOffice API script. Make sure Document Server is running.'));
            };

            document.head.appendChild(script);
        });
    };

    const initializeEditor = (config) => {
        try {
            // Clear previous editor
            if (editorRef.current) {
                editorRef.current.innerHTML = '';
            }

            // OnlyOffice editor configuration
            const editorConfig = {
                documentServerUrl: config.documentServerUrl,
                document: config.document,
                documentType: config.documentType,
                editorConfig: {
                    ...config.editorConfig,
                    callbackUrl: config.editorConfig.callbackUrl,
                    // Custom callback để handle events
                    customization: {
                        ...config.editorConfig.customization,
                        goback: {
                            url: '#',
                            text: 'Đóng Editor'
                        }
                    }
                },
                width: config.width,
                height: config.height,
                events: {
                    onAppReady: () => {
                        console.log('OnlyOffice editor ready');
                        setLoading(false);
                    },
                    onDocumentStateChange: (event) => {
                        console.log('Document state changed:', event);
                    },
                    onError: (event) => {
                        console.error('OnlyOffice editor error:', event);
                        setError('Lỗi editor: ' + (event.data || 'Unknown error'));
                        setLoading(false);
                    },
                    onRequestSaveAs: (event) => {
                        console.log('Save as requested:', event);
                    },
                    onDownloadAs: (event) => {
                        console.log('Download as requested:', event);
                    }
                }
            };

            // Add JWT token nếu có
            if (config.token) {
                editorConfig.token = config.token;
            }

            // Initialize editor
            const editor = new window.DocsAPI.DocEditor('onlyoffice-editor', editorConfig);
            setEditorInstance(editor);

        } catch (err) {
            console.error('Error initializing editor:', err);
            setError('Không thể khởi tạo editor: ' + err.message);
            setLoading(false);
        }
    };

    const handleSave = () => {
        if (editorInstance) {
            // Force save document
            editorInstance.processSaveResult(true);
        }
        if (onSave) {
            onSave();
        }
    };

    const handleClose = () => {
        if (editorInstance) {
            try {
                editorInstance.destroyEditor();
            } catch (e) {
                console.warn('Error destroying editor on close:', e);
            }
        }
        if (onClose) {
            onClose();
        }
    };

    const handleDownload = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/onlyoffice/files/${templateId}`);
            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `template_${templateId}.docx`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            }
        } catch (err) {
            console.error('Error downloading file:', err);
            setError('Không thể tải file: ' + err.message);
        }
    };

    if (error) {
        return (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
                    <h3 className="text-lg font-semibold text-red-600 mb-4">Lỗi OnlyOffice Editor</h3>
                    <p className="text-gray-700 mb-4">{error}</p>
                    <div className="flex gap-2">
                        <button
                            onClick={() => setError('')}
                            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                        >
                            Thử lại
                        </button>
                        <button
                            onClick={handleClose}
                            className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700"
                        >
                            Đóng
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="fixed inset-0 bg-white z-50">
            {/* Header toolbar */}
            <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-gray-50">
                <div className="flex items-center gap-4">
                    <h2 className="text-lg font-semibold text-gray-900">
                        OnlyOffice Editor - Template {templateId}
                    </h2>
                    {loading && (
                        <div className="flex items-center gap-2 text-blue-600">
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                            <span className="text-sm">Đang tải editor...</span>
                        </div>
                    )}
                </div>

                <div className="flex items-center gap-2">
                    <button
                        onClick={handleSave}
                        disabled={loading}
                        className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
                        title="Lưu document"
                    >
                        <Save className="w-4 h-4" />
                        Lưu
                    </button>

                    <button
                        onClick={handleDownload}
                        disabled={loading}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
                        title="Tải xuống"
                    >
                        <Download className="w-4 h-4" />
                        Tải xuống
                    </button>

                    <button
                        onClick={handleClose}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700"
                        title="Đóng editor"
                    >
                        <X className="w-4 h-4" />
                        Đóng
                    </button>
                </div>
            </div>

            {/* OnlyOffice Editor Container */}
            <div className="flex-1 h-full">
                <div
                    id="onlyoffice-editor"
                    ref={editorRef}
                    className="w-full h-full"
                    style={{ height: 'calc(100vh - 80px)' }}
                />
            </div>

            {/* Loading overlay */}
            {loading && (
                <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center">
                    <div className="text-center">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                        <p className="text-gray-600">Đang khởi tạo OnlyOffice Editor...</p>
                        <p className="text-sm text-gray-500 mt-2">
                            Đảm bảo OnlyOffice Document Server đang chạy tại 192.168.100.244:8081
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default OnlyOfficeEditor;