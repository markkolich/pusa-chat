'use strict';

const {rm} = require('fs/promises');
const gulp = require('gulp'),
    concat = require('gulp-concat'),
    eslint = require('gulp-eslint-new'),
    terser = require('gulp-terser'),
    cleanCSS = require('gulp-clean-css'),
    rename = require('gulp-rename'),
    header = require('gulp-header');

async function clean() {
    await rm('build', {recursive: true, force: true});
    await rm('release', {recursive: true, force: true});
}

function concatCss() {
    const cssResources = [
        'css/pusachat.css'
    ];

    return gulp.src(cssResources)
        .pipe(concat('app.css'))
        .pipe(gulp.dest('build'));
}

function minifyCss() {
    return gulp.src('build/app.css')
        .pipe(cleanCSS())
        .pipe(rename({suffix: '.min'}))
        .pipe(gulp.dest('release'));
}

function eslintJs() {
    return gulp.src('js/pusachat/**/*.js')
        .pipe(eslint())
        .pipe(eslint.format())
        .pipe(eslint.failAfterError());
}

function concatJs() {
    const jsResources = [
        'vendor/json2/json2.js',
        'vendor/jquery/jquery-1.7.1.min.js',
        'vendor/jquery.chrono/jquery.chrono-1.1.js',
        'vendor/jquery.simplemodal/jquery.simplemodal-1.4.1.js',
        'vendor/jquery.titlealert/jquery.titlealert-0.7.js',
        'vendor/jquery.typing/jquery.typing-0.2.0.min.js',
        'vendor/jquery.localtime/jquery.localtime-0.5.js',
        'js/pusachat/pusachat.js',
        'js/pusachat/app/chat.js',
        'js/pusachat/app/homepage.js'
    ];

    return gulp.src(jsResources)
        .pipe(concat('app.js'))
        .pipe(gulp.dest('build'));
}

function minifyJs() {
    return gulp.src('build/app.js')
        .pipe(terser())
        .pipe(rename({suffix: '.min'}))
        .pipe(gulp.dest('release'));
}

function addBanner() {
    const comment =
        '/*\n' +
        ' * (c) Copyright 2026 Mark S. Kolich\n' +
        ' * All rights reserved.\n' +
        ' * https://mark.koli.ch\n' +
        ' */\n';

    return gulp.src(['release/*.css', 'release/*.js'])
        .pipe(header(comment))
        .pipe(gulp.dest('release'));
}

// Watch & rebuild on any changes to js/html files
function dev() {
    gulp.watch('css/**/*.css', concatCss);
    gulp.watch('js/**/*.js', gulp.series(eslintJs, concatJs));
}

const buildCss = gulp.series(concatCss, minifyCss);
const buildJs = gulp.series(eslintJs, concatJs, minifyJs);
const release = gulp.series(clean, gulp.parallel(buildCss, buildJs), addBanner);

gulp.task('clean', clean);
gulp.task('concat-css', concatCss);
gulp.task('minify-css', gulp.series(concatCss, minifyCss));
gulp.task('eslint-js', eslintJs);
gulp.task('concat-js', gulp.series(eslintJs, concatJs));
gulp.task('minify-js', gulp.series(eslintJs, concatJs, minifyJs));
gulp.task('banner', gulp.series(buildCss, buildJs, addBanner));
gulp.task('dev', gulp.series(gulp.parallel(concatCss, gulp.series(eslintJs, concatJs)), dev));
gulp.task('release', release);
gulp.task('default', gulp.task('dev'));
