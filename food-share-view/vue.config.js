module.exports = {
    publicPath: './',
    devServer: {
        port: 21091,
        proxy: {
            '/api': {
                target: 'http://localhost:21090',
                changeOrigin: true,
                pathRewrite: {
                    '^/api': '/api'
                }
            }
        },
        // 关闭ESLint检查
        overlay: {
            warnings: false,
            errors: true
        }
    },
    // 关闭ESLint检查
    lintOnSave: false,
    configureWebpack: {
        performance: {
            hints: false
        },
        // 优化图片处理
        module: {
            rules: [
                {
                    test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
                    use: [
                        {
                            loader: 'url-loader',
                            options: {
                                limit: 10000,
                                name: 'static/img/[name].[hash:7].[ext]'
                            }
                        }
                    ]
                }
            ]
        }
    },
    // 启用模板编译器
    runtimeCompiler: true,
    // 不生成 source map
    productionSourceMap: false
}; 