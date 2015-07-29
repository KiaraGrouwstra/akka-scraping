gulp = require('gulp')
jade = require('gulp-jade')

# path =
#   js: [ './app/assets/javascripts/**/*.js' ]
#   template: [ './app/assets/**/*.tpl.html' ]
#   watch: [ 'app/assets/**/*.*' ]

gulp.task 'templates', ->
  gulp.src('./app/**/*.jade').pipe(jade(locals: 'pretty': true)).pipe gulp.dest('./app/')
  return

gulp.task 'css', ->
  postcss = require('gulp-postcss')
  # sourcemaps = require('gulp-sourcemaps')
  # autoprefixer = require('autoprefixer-core')
  autoprefixer = require('autoprefixer')
  gulp.src('./app/**/*.css')
    # .pipe(sourcemaps.init())
    .pipe(postcss([
      require('cssnext')()
      require('cssnano')()
      autoprefixer(browsers: [ 'last 2 versions' ])
    ]))
    # .pipe(sourcemaps.write('.'))
    .pipe gulp.dest('./app/')

gulp.task 'build', ->
  gulp.run [ 'templates' ]
  # gulp.run [ 'css' ]
  # gulp.watch(path.watch, ['templates']);
  return

gulp.task 'default', ->
  gulp.run [ 'build' ]
  return

# ngAnnotate = require('gulp-ng-annotate')
# concat = require('gulp-concat')
# uglify = require('gulp-uglify')
# ngHtml2Js = require('gulp-ng-html2js')
# minifyHtml = require('gulp-minify-html')
# plumber = require('gulp-plumber')
# gulp.task 'js:compile', ->
#   gulp.src(path.js).pipe(plumber()).pipe(ngAnnotate()).pipe(concat('app.js')).pipe gulp.dest('./public/javascripts/')
#   return
# gulp.task 'js:minifyCompile', ->
#   gulp.src(path.js).pipe(plumber()).pipe(ngAnnotate()).pipe(concat('app.min.js')).pipe(uglify()).pipe gulp.dest('./public/javascripts/')
#   return
# gulp.task 'template:compile', ->
#   gulp.src(path.template).pipe(plumber()).pipe(ngHtml2Js(
#     moduleName: 'app.tpl'
#     prefix: '/')).pipe(concat('app.tpl.js')).pipe gulp.dest('./public/javascripts/')
#   return
# gulp.task 'template:minifyCompile', ->
#   gulp.src(path.template).pipe(plumber()).pipe(minifyHtml(
#     empty: true
#     space: true
#     quotes: true)).pipe(ngHtml2Js(
#     moduleName: 'app.tpl'
#     prefix: '/')).pipe(concat('app.tpl.min.js')).pipe(uglify()).pipe gulp.dest('./public/javascripts/')
#   return
# gulp.task 'default', ->
#   gulp.run [
#     'js:compile'
#     'js:minifyCompile'
#     'template:compile'
#     'template:minifyCompile'
#   ]
#   gulp.watch path.watch, [
#     'js:compile'
#     'js:minifyCompile'
#     'template:compile'
#     'template:minifyCompile'
#   ]
#   return
