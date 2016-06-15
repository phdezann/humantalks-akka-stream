var path = require('path'),
    jsPath  = 'app/assets/javascripts',
    srcPath = path.join(__dirname, jsPath);

module.exports = {
  entry: {
    app: path.join(srcPath, 'app.js'),
  },
  output: {
    path: path.resolve(__dirname, jsPath, 'build'),
    publicPath: '',
    filename: '[name].js',
    pathInfo: true
  },
  module: {
      noParse: [],
      loaders: [
          {
              test: /\.js$/,
              loader: 'babel',
              exclude: /node_modules/
          }
      ]
  }
}
