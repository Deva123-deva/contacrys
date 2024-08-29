(function(f){if(typeof exports==="object"&&typeof module!=="undefined"&&false){module.exports=f()}else if(typeof define==="function"&&define.amd&&false){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.ejs=f()}})(function(){var define,module,exports;return function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r}()({1:[function(require,module,exports){"use strict";var fs=require("fs");var path=require("path");var utils=require("./utils");var scopeOptionWarned=false;var _VERSION_STRING=require("../package.json").version;var _DEFAULT_OPEN_DELIMITER="<";var _DEFAULT_CLOSE_DELIMITER=">";var _DEFAULT_DELIMITER="%";var _DEFAULT_LOCALS_NAME="locals";var _NAME="ejs";var _REGEX_STRING="(<%%|%%>|<%=|<%-|<%_|<%#|<%|%>|-%>|_%>)";var _OPTS_PASSABLE_WITH_DATA=["delimiter","scope","context","debug","compileDebug","client","_with","rmWhitespace","strict","filename","async"];var _OPTS_PASSABLE_WITH_DATA_EXPRESS=_OPTS_PASSABLE_WITH_DATA.concat("cache");var _BOM=/^\uFEFF/;exports.cache=utils.cache;exports.fileLoader=fs.readFileSync;exports.localsName=_DEFAULT_LOCALS_NAME;exports.promiseImpl=new Function("return this;")().Promise;exports.resolveInclude=function(name,filename,isDir){var dirname=path.dirname;var extname=path.extname;var resolve=path.resolve;var includePath=resolve(isDir?filename:dirname(filename),name);var ext=extname(name);if(!ext){includePath+=".ejs"}return includePath};function resolvePaths(name,paths){var filePath;if(paths.some(function(v){filePath=exports.resolveInclude(name,v,true);return fs.existsSync(filePath)})){return filePath}}function getIncludePath(path,options){var includePath;var filePath;var views=options.views;var match=/^[A-Za-z]+:\\|^\//.exec(path);if(match&&match.length){path=path.replace(/^\/*/,"");if(Array.isArray(options.root)){includePath=resolvePaths(path,options.root)}else{includePath=exports.resolveInclude(path,options.root||"/",true)}}else{if(options.filename){filePath=exports.resolveInclude(path,options.filename);if(fs.existsSync(filePath)){includePath=filePath}}if(!includePath&&Array.isArray(views)){includePath=resolvePaths(path,views)}if(!includePath&&typeof options.includer!=="function"){throw new Error('Could not find the include file "'+options.escapeFunction(path)+'"')}}return includePath}function handleCache(options,template){var func;var filename=options.filename;var hasTemplate=arguments.length>1;if(options.cache){if(!filename){throw new Error("cache option requires a filename")}func=exports.cache.get(filename);if(func){return func}if(!hasTemplate){template=fileLoader(filename).toString().replace(_BOM,"")}}else if(!hasTemplate){if(!filename){throw new Error("Internal EJS error: no file name or template "+"provided")}template=fileLoader(filename).toString().replace(_BOM,"")}func=exports.compile(template,options);if(options.cache){exports.cache.set(filename,func)}return func}function tryHandleCache(options,data,cb){var result;if(!cb){if(typeof exports.promiseImpl=="function"){return new exports.promiseImpl(function(resolve,reject){try{result=handleCache(options)(data);resolve(result)}catch(err){reject(err)}})}else{throw new Error("Please provide a callback function")}}else{try{result=handleCache(options)(data)}catch(err){return cb(err)}cb(null,result)}}function fileLoader(filePath){return exports.fileLoader(filePath)}function includeFile(path,options){var opts=utils.shallowCopy({},options);opts.filename=getIncludePath(path,opts);if(typeof options.includer==="function"){var includerResult=options.includer(path,opts.filename);if(includerResult){if(includerResult.filename){opts.filename=includerResult.filename}if(includerResult.template){return handleCache(opts,includerResult.template)}}}return handleCache(opts)}function rethrow(err,str,flnm,lineno,esc){var lines=str.split("\n");var start=Math.max(lineno-3,0);var end=Math.min(lines.length,lineno+3);var filename=esc(flnm);var context=lines.slice(start,end).map(function(line,i){var curr=i+start+1;return(curr==lineno?" >> ":"    ")+curr+"| "+line}).join("\n");err.path=filename;err.message=(filename||"ejs")+":"+lineno+"\n"+context+"\n\n"+err.message;throw err}function stripSemi(str){return str.replace(/;(\s*$)/,"$1")}exports.compile=function compile(template,opts){var templ;if(opts&&opts.scope){if(!scopeOptionWarned){console.warn("`scope` option is deprecated and will be removed in EJS 3");scopeOptionWarned=true}if(!opts.context){opts.context=opts.scope}delete opts.scope}templ=new Template(template,opts);return templ.compile()};exports.render=function(template,d,o){var data=d||{};var opts=o||{};if(arguments.length==2){utils.shallowCopyFromList(opts,data,_OPTS_PASSABLE_WITH_DATA)}return handleCache(opts,template)(data)};exports.renderFile=function(){var args=Array.prototype.slice.call(arguments);var filename=args.shift();var cb;var opts={filename:filename};var data;var viewOpts;if(typeof arguments[arguments.length-1]=="function"){cb=args.pop()}if(args.length){data=args.shift();if(args.length){utils.shallowCopy(opts,args.pop())}else{if(data.settings){if(data.settings.views){opts.views=data.settings.views}if(data.settings["view cache"]){opts.cache=true}viewOpts=data.settings["view options"];if(viewOpts){utils.shallowCopy(opts,viewOpts)}}utils.shallowCopyFromList(opts,data,_OPTS_PASSABLE_WITH_DATA_EXPRESS)}opts.filename=filename}else{data={}}return tryHandleCache(opts,data,cb)};exports.Template=Template;exports.clearCache=function(){exports.cache.reset()};function Template(text,opts){opts=opts||{};var options={};this.templateText=text;this.mode=null;this.truncate=false;this.currentLine=1;this.source="";options.client=opts.client||false;options.escapeFunction=opts.escape||opts.escapeFunction||utils.escapeXML;options.compileDebug=opts.compileDebug!==false;options.debug=!!opts.debug;options.filename=opts.filename;options.openDelimiter=opts.openDelimiter||exports.openDelimiter||_DEFAULT_OPEN_DELIMITER;options.closeDelimiter=opts.closeDelimiter||exports.closeDelimiter||_DEFAULT_CLOSE_DELIMITER;options.delimiter=opts.delimiter||exports.delimiter||_DEFAULT_DELIMITER;options.strict=opts.strict||false;options.context=opts.context;options.cache=opts.cache||false;options.rmWhitespace=opts.rmWhitespace;options.root=opts.root;options.includer=opts.includer;options.outputFunctionName=opts.outputFunctionName;options.localsName=opts.localsName||exports.localsName||_DEFAULT_LOCALS_NAME;options.views=opts.views;options.async=opts.async;options.destructuredLocals=opts.destructuredLocals;options.legacyInclude=typeof opts.legacyInclude!="undefined"?!!opts.legacyInclude:true;if(options.strict){options._with=false}else{options._with=typeof opts._with!="undefined"?opts._with:true}this.opts=options;this.regex=this.createRegex()}Template.modes={EVAL:"eval",ESCAPED:"escaped",RAW:"raw",COMMENT:"comment",LITERAL:"literal"};Template.prototype={createRegex:function(){var str=_REGEX_STRING;var delim=utils.escapeRegExpChars(this.opts.delimiter);var open=utils.escapeRegExpChars(this.opts.openDelimiter);var close=utils.escapeRegExpChars(this.opts.closeDelimiter);str=str.replace(/%/g,delim).replace(/</g,open).replace(/>/g,close);return new RegExp(str)},compile:function(){var src;var fn;var opts=this.opts;var prepended="";var appended="";var escapeFn=opts.escapeFunction;var ctor;var sanitizedFilename=opts.filename?JSON.stringify(opts.filename):"undefined";if(!this.source){this.generateSource();prepended+='  var __output = "";\n'+"  function __append(s) { if (s !== undefined && s !== null) __output += s }\n";if(opts.outputFunctionName){prepended+="  var "+opts.outputFunctionName+" = __append;"+"\n"}if(opts.destructuredLocals&&opts.destructuredLocals.length){var destructuring="  var __locals = ("+opts.localsName+" || {}),\n";for(var i=0;i<opts.destructuredLocals.length;i++){var name=opts.destructuredLocals[i];if(i>0){destructuring+=",\n  "}destructuring+=name+" = __locals."+name}prepended+=destructuring+";\n"}if(opts._with!==false){prepended+="  with ("+opts.localsName+" || {}) {"+"\n";appended+="  }"+"\n"}appended+="  return __output;"+"\n";this.source=prepended+this.source+appended}if(opts.compileDebug){src="var __line = 1"+"\n"+"  , __lines = "+JSON.stringify(this.templateText)+"\n"+"  , __filename = "+sanitizedFilename+";"+"\n"+"try {"+"\n"+this.source+"} catch (e) {"+"\n"+"  rethrow(e, __lines, __filename, __line, escapeFn);"+"\n"+"}"+"\n"}else{src=this.source}if(opts.client){src="escapeFn = escapeFn || "+escapeFn.toString()+";"+"\n"+src;if(opts.compileDebug){src="rethrow = rethrow || "+rethrow.toString()+";"+"\n"+src}}if(opts.strict){src='"use strict";\n'+src}if(opts.debug){console.log(src)}if(opts.compileDebug&&opts.filename){src=src+"\n"+"//# sourceURL="+sanitizedFilename+"\n"}try{if(opts.async){try{ctor=new Function("return (async function(){}).constructor;")()}catch(e){if(e instanceof SyntaxError){throw new Error("This environment does not support async/await")}else{throw e}}}else{ctor=Function}fn=new ctor(opts.localsName+", escapeFn, include, rethrow",src)}catch(e){if(e instanceof SyntaxError){if(opts.filename){e.message+=" in "+opts.filename}e.message+=" while compiling ejs\n\n";e.message+="If the above error is not helpful, you may want to try EJS-Lint:\n";e.message+="https://github.com/RyanZim/EJS-Lint";if(!opts.async){e.message+="\n";e.message+="Or, if you meant to create an async function, pass `async: true` as an option."}}throw e}var returnedFn=opts.client?fn:function anonymous(data){var include=function(path,includeData){var d=utils.shallowCopy({},data);if(includeData){d=utils.shallowCopy(d,includeData)}return includeFile(path,opts)(d)};return fn.apply(opts.context,[data||{},escapeFn,include,rethrow])};if(opts.filename&&typeof Object.defineProperty==="function"){var filename=opts.filename;var basename=path.basename(filename,path.extname(filename));try{Object.defineProperty(returnedFn,"name",{value:basename,writable:false,enumerable:false,configurable:true})}catch(e){}}return returnedFn},generateSource:function(){var opts=this.opts;if(opts.rmWhitespace){this.templateText=this.templateText.replace(/[\r\n]+/g,"\n").replace(/^\s+|\s+$/gm,"")}this.templateText=this.templateText.replace(/[ \t]*<%_/gm,"<%_").replace(/_%>[ \t]*/gm,"_%>");var self=this;var matches=this.parseTemplateText();var d=this.opts.delimiter;var o=this.opts.openDelimiter;var c=this.opts.closeDelimiter;if(matches&&matches.length){matches.forEach(function(line,index){var closing;if(line.indexOf(o+d)===0&&line.indexOf(o+d+d)!==0){closing=matches[index+2];if(!(closing==d+c||closing=="-"+d+c||closing=="_"+d+c)){throw new Error('Could not find matching close tag for "'+line+'".')}}self.scanLine(line)})}},parseTemplateText:function(){var str=this.templateText;var pat=this.regex;var result=pat.exec(str);var arr=[];var firstPos;while(result){firstPos=result.index;if(firstPos!==0){arr.push(str.substring(0,firstPos));str=str.slice(firstPos)}arr.push(result[0]);str=str.slice(result[0].length);result=pat.exec(str)}if(str){arr.push(str)}return arr},_addOutput:function(line){if(this.truncate){line=line.replace(/^(?:\r\n|\r|\n)/,"");this.truncate=false}if(!line){return line}line=line.replace(/\\/g,"\\\\");line=line.replace(/\n/g,"\\n");line=line.replace(/\r/g,"\\r");line=line.replace(/"/g,'\\"');this.source+='    ; __append("'+line+'")'+"\n"},scanLine:function(line){var self=this;var d=this.opts.delimiter;var o=this.opts.openDelimiter;var c=this.opts.closeDelimiter;var newLineCount=0;newLineCount=line.split("\n").length-1;switch(line){case o+d:case o+d+"_":this.mode=Template.modes.EVAL;break;case o+d+"=":this.mode=Template.modes.ESCAPED;break;case o+d+"-":this.mode=Template.modes.RAW;break;case o+d+"#":this.mode=Template.modes.COMMENT;break;case o+d+d:this.mode=Template.modes.LITERAL;this.source+='    ; __append("'+line.replace(o+d+d,o+d)+'")'+"\n";break;case d+d+c:this.mode=Template.modes.LITERAL;this.source+='    ; __append("'+line.replace(d+d+c,d+c)+'")'+"\n";break;case d+c:case"-"+d+c:case"_"+d+c:if(this.mode==Template.modes.LITERAL){this._addOutput(line)}this.mode=null;this.truncate=line.indexOf("-")===0||line.indexOf("_")===0;break;default:if(this.mode){switch(this.mode){case Template.modes.EVAL:case Template.modes.ESCAPED:case Template.modes.RAW:if(line.lastIndexOf("//")>line.lastIndexOf("\n")){line+="\n"}}switch(this.mode){case Template.modes.EVAL:this.source+="    ; "+line+"\n";break;case Template.modes.ESCAPED:this.source+="    ; __append(escapeFn("+stripSemi(line)+"))"+"\n";break;case Template.modes.RAW:this.source+="    ; __append("+stripSemi(line)+")"+"\n";break;case Template.modes.COMMENT:break;case Template.modes.LITERAL:this._addOutput(line);break}}else{this._addOutput(line)}}if(self.opts.compileDebug&&newLineCount){this.currentLine+=newLineCount;this.source+="    ; __line = "+this.currentLine+"\n"}}};exports.escapeXML=utils.escapeXML;exports.__express=exports.renderFile;exports.VERSION=_VERSION_STRING;exports.name=_NAME;if(typeof window!="undefined"){window.ejs=exports}},{"../package.json":6,"./utils":2,fs:3,path:4}],2:[function(require,module,exports){"use strict";var regExpChars=/[|\\{}()[\]^$+*?.]/g;exports.escapeRegExpChars=function(string){if(!string){return""}return String(string).replace(regExpChars,"\\$&")};var _ENCODE_HTML_RULES={"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&#34;","'":"&#39;"};var _MATCH_HTML=/[&<>'"]/g;function encode_char(c){return _ENCODE_HTML_RULES[c]||c}var escapeFuncStr="var _ENCODE_HTML_RULES = {\n"+'      "&": "&amp;"\n'+'    , "<": "&lt;"\n'+'    , ">": "&gt;"\n'+'    , \'"\': "&#34;"\n'+'    , "\'": "&#39;"\n'+"    }\n"+"  , _MATCH_HTML = /[&<>'\"]/g;\n"+"function encode_char(c) {\n"+"  return _ENCODE_HTML_RULES[c] || c;\n"+"};\n";exports.escapeXML=function(markup){return markup==undefined?"":String(markup).replace(_MATCH_HTML,encode_char)};exports.escapeXML.toString=function(){return Function.prototype.toString.call(this)+";\n"+escapeFuncStr};exports.shallowCopy=function(to,from){from=from||{};for(var p in from){to[p]=from[p]}return to};exports.shallowCopyFromList=function(to,from,list){for(var i=0;i<list.length;i++){var p=list[i];if(typeof from[p]!="undefined"){to[p]=from[p]}}return to};exports.cache={_data:{},set:function(key,val){this._data[key]=val},get:function(key){return this._data[key]},remove:function(key){delete this._data[key]},reset:function(){this._data={}}};exports.hyphenToCamel=function(str){return str.replace(/-[a-z]/g,function(match){return match[1].toUpperCase()})}},{}],3:[function(require,module,exports){},{}],4:[function(require,module,exports){(function(process){function normalizeArray(parts,allowAboveRoot){var up=0;for(var i=parts.length-1;i>=0;i--){var last=parts[i];if(last==="."){parts.splice(i,1)}else if(last===".."){parts.splice(i,1);up++}else if(up){parts.splice(i,1);up--}}if(allowAboveRoot){for(;up--;up){parts.unshift("..")}}return parts}exports.resolve=function(){var resolvedPath="",resolvedAbsolute=false;for(var i=arguments.length-1;i>=-1&&!resolvedAbsolute;i--){var path=i>=0?arguments[i]:process.cwd();if(typeof path!=="string"){throw new TypeError("Arguments to path.resolve must be strings")}else if(!path){continue}resolvedPath=path+"/"+resolvedPath;resolvedAbsolute=path.charAt(0)==="/"}resolvedPath=normalizeArray(filter(resolvedPath.split("/"),function(p){return!!p}),!resolvedAbsolute).join("/");return(resolvedAbsolute?"/":"")+resolvedPath||"."};exports.normalize=function(path){var isAbsolute=exports.isAbsolute(path),trailingSlash=substr(path,-1)==="/";path=normalizeArray(filter(path.split("/"),function(p){return!!p}),!isAbsolute).join("/");if(!path&&!isAbsolute){path="."}if(path&&trailingSlash){path+="/"}return(isAbsolute?"/":"")+path};exports.isAbsolute=function(path){return path.charAt(0)==="/"};exports.join=function(){var paths=Array.prototype.slice.call(arguments,0);return exports.normalize(filter(paths,function(p,index){if(typeof p!=="string"){throw new TypeError("Arguments to path.join must be strings")}return p}).join("/"))};exports.relative=function(from,to){from=exports.resolve(from).substr(1);to=exports.resolve(to).substr(1);function trim(arr){var start=0;for(;start<arr.length;start++){if(arr[start]!=="")break}var end=arr.length-1;for(;end>=0;end--){if(arr[end]!=="")break}if(start>end)return[];return arr.slice(start,end-start+1)}var fromParts=trim(from.split("/"));var toParts=trim(to.split("/"));var length=Math.min(fromParts.length,toParts.length);var samePartsLength=length;for(var i=0;i<length;i++){if(fromParts[i]!==toParts[i]){samePartsLength=i;break}}var outputParts=[];for(var i=samePartsLength;i<fromParts.length;i++){outputParts.push("..")}outputParts=outputParts.concat(toParts.slice(samePartsLength));return outputParts.join("/")};exports.sep="/";exports.delimiter=":";exports.dirname=function(path){if(typeof path!=="string")path=path+"";if(path.length===0)return".";var code=path.charCodeAt(0);var hasRoot=code===47;var end=-1;var matchedSlash=true;for(var i=path.length-1;i>=1;--i){code=path.charCodeAt(i);if(code===47){if(!matchedSlash){end=i;break}}else{matchedSlash=false}}if(end===-1)return hasRoot?"/":".";if(hasRoot&&end===1){return"/"}return path.slice(0,end)};function basename(path){if(typeof path!=="string")path=path+"";var start=0;var end=-1;var matchedSlash=true;var i;for(i=path.length-1;i>=0;--i){if(path.charCodeAt(i)===47){if(!matchedSlash){start=i+1;break}}else if(end===-1){matchedSlash=false;end=i+1}}if(end===-1)return"";return path.slice(start,end)}exports.basename=function(path,ext){var f=basename(path);if(ext&&f.substr(-1*ext.length)===ext){f=f.substr(0,f.length-ext.length)}return f};exports.extname=function(path){if(typeof path!=="string")path=path+"";var startDot=-1;var startPart=0;var end=-1;var matchedSlash=true;var preDotState=0;for(var i=path.length-1;i>=0;--i){var code=path.charCodeAt(i);if(code===47){if(!matchedSlash){startPart=i+1;break}continue}if(end===-1){matchedSlash=false;end=i+1}if(code===46){if(startDot===-1)startDot=i;else if(preDotState!==1)preDotState=1}else if(startDot!==-1){preDotState=-1}}if(startDot===-1||end===-1||preDotState===0||preDotState===1&&startDot===end-1&&startDot===startPart+1){return""}return path.slice(startDot,end)};function filter(xs,f){if(xs.filter)return xs.filter(f);var res=[];for(var i=0;i<xs.length;i++){if(f(xs[i],i,xs))res.push(xs[i])}return res}var substr="ab".substr(-1)==="b"?function(str,start,len){return str.substr(start,len)}:function(str,start,len){if(start<0)start=str.length+start;return str.substr(start,len)}}).call(this,require("_process"))},{_process:5}],5:[function(require,module,exports){var process=module.exports={};var cachedSetTimeout;var cachedClearTimeout;function defaultSetTimout(){throw new Error("setTimeout has not been defined")}function defaultClearTimeout(){throw new Error("clearTimeout has not been defined")}(function(){try{if(typeof setTimeout==="function"){cachedSetTimeout=setTimeout}else{cachedSetTimeout=defaultSetTimout}}catch(e){cachedSetTimeout=defaultSetTimout}try{if(typeof clearTimeout==="function"){cachedClearTimeout=clearTimeout}else{cachedClearTimeout=defaultClearTimeout}}catch(e){cachedClearTimeout=defaultClearTimeout}})();function runTimeout(fun){if(cachedSetTimeout===setTimeout){return setTimeout(fun,0)}if((cachedSetTimeout===defaultSetTimout||!cachedSetTimeout)&&setTimeout){cachedSetTimeout=setTimeout;return setTimeout(fun,0)}try{return cachedSetTimeout(fun,0)}catch(e){try{return cachedSetTimeout.call(null,fun,0)}catch(e){return cachedSetTimeout.call(this,fun,0)}}}function runClearTimeout(marker){if(cachedClearTimeout===clearTimeout){return clearTimeout(marker)}if((cachedClearTimeout===defaultClearTimeout||!cachedClearTimeout)&&clearTimeout){cachedClearTimeout=clearTimeout;return clearTimeout(marker)}try{return cachedClearTimeout(marker)}catch(e){try{return cachedClearTimeout.call(null,marker)}catch(e){return cachedClearTimeout.call(this,marker)}}}var queue=[];var draining=false;var currentQueue;var queueIndex=-1;function cleanUpNextTick(){if(!draining||!currentQueue){return}draining=false;if(currentQueue.length){queue=currentQueue.concat(queue)}else{queueIndex=-1}if(queue.length){drainQueue()}}function drainQueue(){if(draining){return}var timeout=runTimeout(cleanUpNextTick);draining=true;var len=queue.length;while(len){currentQueue=queue;queue=[];while(++queueIndex<len){if(currentQueue){currentQueue[queueIndex].run()}}queueIndex=-1;len=queue.length}currentQueue=null;draining=false;runClearTimeout(timeout)}process.nextTick=function(fun){var args=new Array(arguments.length-1);if(arguments.length>1){for(var i=1;i<arguments.length;i++){args[i-1]=arguments[i]}}queue.push(new Item(fun,args));if(queue.length===1&&!draining){runTimeout(drainQueue)}};function Item(fun,array){this.fun=fun;this.array=array}Item.prototype.run=function(){this.fun.apply(null,this.array)};process.title="browser";process.browser=true;process.env={};process.argv=[];process.version="";process.versions={};function noop(){}process.on=noop;process.addListener=noop;process.once=noop;process.off=noop;process.removeListener=noop;process.removeAllListeners=noop;process.emit=noop;process.prependListener=noop;process.prependOnceListener=noop;process.listeners=function(name){return[]};process.binding=function(name){throw new Error("process.binding is not supported")};process.cwd=function(){return"/"};process.chdir=function(dir){throw new Error("process.chdir is not supported")};process.umask=function(){return 0}},{}],6:[function(require,module,exports){module.exports={name:"ejs",description:"Embedded JavaScript templates",keywords:["template","engine","ejs"],version:"3.1.6",author:"Matthew Eernisse <mde@fleegix.org> (http://fleegix.org)",license:"Apache-2.0",bin:{ejs:"./bin/cli.js"},main:"./lib/ejs.js",jsdelivr:"ejs.min.js",unpkg:"ejs.min.js",repository:{type:"git",url:"git://github.com/mde/ejs.git"},bugs:"https://github.com/mde/ejs/issues",homepage:"https://github.com/mde/ejs",dependencies:{jake:"^10.6.1"},devDependencies:{browserify:"^16.5.1",eslint:"^6.8.0","git-directory-deploy":"^1.5.1",jsdoc:"^3.6.4","lru-cache":"^4.0.1",mocha:"^7.1.1","uglify-js":"^3.3.16"},engines:{node:">=0.10.0"},scripts:{test:"mocha"}}},{}]},{},[1])(1)});


function FormFacade(data)
{
    this.data = data;
    this.draft = null;
    this.result = null;
    this.template = {};
    this.showago = true;
    this.__sections = null;
    this.paymentIntent = null;
    this.debounceTimer = null;
    this.signaturePad = {}
    this.signatures = { signs: {}}

    this.prefill = function()
    {
        var curr = this;
        this.draft = {};
        if(!this.draft.entry) this.draft.entry = {};
        if(!this.draft.pageHistory) this.draft.pageHistory = [];
        if(!this.draft.activePage) this.draft.activePage = 'root';
        var items = this.data.scraped.items||{};
        var qprefill = this.data.request.query.prefill;
        var fcprefill = this.data.facade.prefill||{};
        for(var prenm in fcprefill)
            if(!fcprefill[prenm]) delete fcprefill[prenm];
        if(qprefill && window[qprefill])
        {
            var rslt = window[qprefill](this);
            for(var itemId in items)
            {
                var item = items[itemId];
                var preval = rslt['entry.'+item.entry];
                if(preval) this.draft.entry[item.entry] = preval;
            }
        }
        else if(Object.keys(fcprefill).length>0)
        {
            this.draft.entry = fcprefill;
        }
        else
        {
            var urlparams = new URLSearchParams(window.location.search);
            var eml = urlparams.get('emailAddress');
            if(eml) this.draft.emailAddress = eml;
            for(var itemId in items)
            {
                var item = items[itemId];
                var urlval = urlparams.get('entry.'+item.entry);
                if(item.type=='CHECKBOX' && urlval)
                {
                    urlval = urlparams.getAll('entry.'+item.entry);
                    curr.draft.entry[item.entry] = urlval;
                }
                else if(item.type=='GRID' && item.rows)
                {
                    item.rows.forEach(function(rw){
                        if(rw.multiple)
                            urlval = urlparams.getAll('entry.'+rw.entry);
                        else
                            urlval = urlparams.get('entry.'+rw.entry);
                        if(urlval)
                            curr.draft.entry[rw.entry] = urlval;
                    });
                }
                else if(urlval)
                {
                    curr.draft.entry[item.entry] = urlval;
                }
                var urlothr = urlparams.get('entry.'+item.entry+'.other_option_response');
                if(urlothr) curr.draft.entry[item.entry+'-other_option_response'] = urlothr;
            }
        }
        return this.draft;
    }

    this.computeField = function(tmpl = '', citm)
    {
        const regex = /<img\s+[^>]*src="[^"]+\.html(\?[^"]*)?"/gi;
        if(tmpl.match(regex)) return 'Image should not have html';
        if(!citm && tmpl.indexOf('${')<0) return tmpl;
        return this.calculateEngine(tmpl, {calcfield:citm});
    }

    this.compute = function()
    {
        var curr = this;
        var items = this.data.scraped.items||{};
        var oitems = this.data.facade.items||{};
        var sitems = [];
        for(var sid in items)
        {
            var sitm = items[sid];
            sitm.id = sid;
            sitm.logic = oitems[sid];
            sitems.push(sitm);
        }
        sitems.sort(function(a,b){ return a.index-b.index; });
        sitems.forEach(function(item, i){
            var itemId = item.id;
            var oitem = oitems[itemId];
            if(oitem)
            {
                if(oitem.calculated)
                {
                    var calcval = curr.computeField(oitem.calculated, item);
                    if(curr.draft && curr.draft.entry)
                        curr.draft.entry[item.entry] = calcval;
                    var widg = document.getElementById('Widget'+itemId);
                    if(widg) widg.value = calcval;
                    var disp = document.getElementById('Display'+itemId);
                    if(disp)
                    {
                        if(calcval && item.type=='DATE')
                        {
                            var b = calcval.split(/\D/);
                            var calcdt = new Date(0, 0, 0);
                            if(b.length==3)
                                calcdt = new Date(b[0], b[1]-1, b[2]);
                            else if(b.length==6)
                                calcdt = new Date(b[0], b[1]-1, b[2], b[3], b[4], b[5]);
                            if(item.time==1)
                                disp.value = calcdt.toLocaleString();
                            else
                                disp.value = calcdt.toLocaleDateString();
                        }
                        else
                            disp.value = item.format?item.format(calcval):calcval;
                    }
                }
                else if(oitem.prefill && !curr.draft.entry[item.entry])
                {
                    var preval = curr.computeField(oitem.prefill, item);
                    if(preval)
                    {
                        curr.draft.entry[item.entry] = preval;
                        var widg = document.getElementById('Widget'+itemId);
                        if(widg) widg.value = preval;
                        var disp = document.getElementById('Display'+itemId);
                        if(disp) disp.value = preval;
                    }
                }
                else if(oitem.type=='FILE_UPLOAD' && oitem.subtype != 'SIGNATURE')
                {
                    var files = curr.draft.entry[item.entry];
                    var widg = document.getElementById('Widget'+itemId);
                    if(widg && files) widg.value = files;
                    var filearr = [];
                    if(files) filearr = files.split(',');
                    filearr = filearr.map(function(fl){ 
                        var fnm = decodeURIComponent(fl.split('/').pop().trim());
                        return '<a class="addedfile" href="javascript:void(0)">'+fnm+'</a>'; 
                    });
                    var disp = document.getElementById('Display'+itemId);
                    if(disp)
                    {
                        if(filearr.length>0)
                            disp.innerHTML = filearr.join(' ');
                        else
                        {
                            var plchdr = oitem.placeholder?oitem.placeholder:'Add file';
                            disp.innerHTML = '<a class="addfile" href="javascript:void(0)">'+plchdr+'</a>';
                        }
                    }
                }
            }
        });
        sitems.forEach(function(item, i){
            var itemId = item.id;
            var oitem = oitems[itemId];
            var widg = document.getElementById('Help'+itemId);
            if(oitem && oitem.helpMark && widg)
            {
                var preval = curr.computeField(oitem.helpMark, item);
                widg.innerHTML = preval;
            }
        });
        var doc = this.getDocument();
        var ttls = this.data.facade&&this.data.facade.titles?this.data.facade.titles:{};
        for(var titleId in ttls)
        {
            var ttl = ttls[titleId];
            var ttldiv = doc.getElementById('ff-desc-'+titleId);
            if(ttl.messageMark && ttldiv)
            {
                var deschtm = curr.computeField(ttl.messageMark);
                ttldiv.innerHTML = curr.switchAllCDN(deschtm);
            }
        }
        doc.querySelectorAll(
            '.ff-title a, .ff-section-header a, .ff-description a, .ff-item label a, .ff-item .ff-help a'
        ).forEach(lnk=>{
            if(lnk.classList.length==0) lnk.target = '_blank';
        });
    }

    this.toRGB = function(hex, opacity) {
        var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        if(result)
        {
            var rgb = [
                parseInt(result[1], 16),
                parseInt(result[2], 16),
                parseInt(result[3], 16)
            ];
            if(opacity) rgb.push(opacity);
            return 'rgb('+rgb.join(', ')+')';
        }
        return hex;

    }

    this.getEnhancement = function()
    {
        var enhance = this.data.request.query.enhance;
        if(enhance == 'yes')
        {
            return {
                layout:'1column', color:'theme', font:'space',
                input:'flat', button:'flat'
            };
        }
        return null;
    }

    this.shuffle = function(array)
    {
        if(this.isEditMode()) return array;
        var currentIndex = array.length, temporaryValue, randomIndex;
        while (0 !== currentIndex) 
        {
            randomIndex = Math.floor(Math.random() * currentIndex);
            currentIndex -= 1;
            temporaryValue = array[currentIndex];
            array[currentIndex] = array[randomIndex];
            array[randomIndex] = temporaryValue;
        }
        return array;
    }

    this.filter = function(chs=[])
    {
        var valids = [];
        var empties = [];
        var invalids = [];
        chs.forEach(function(ch){
          if(ch.value=='__other_option__')
            invalids.push(ch);
          else if(ch.value=='')
            empties.push(ch);
          else
            valids.push(ch);
        });
        return valids.concat(empties);
    }

    this.loadScript = function(jssrc, callback)
    {
        if(document.querySelectorAll(`script[src="${jssrc}"]`).length>0)
            return callback();
        var script = document.createElement("script")
        script.type = "text/javascript";
        if (script.readyState){  //IE
            script.onreadystatechange = function(e){
                if (script.readyState == "loaded" || script.readyState == "complete"){
                    script.onreadystatechange = null;
                    callback(e);
                }
            };
        } else {
            script.onload = script.onerror = function(e){
                callback(e);
            };
        }
        script.src = jssrc;
        document.getElementsByTagName("head")[0].appendChild(script);
    }

    this.loadScripts = function(srcs)
    {
        var curr = this;
        if(this.data.devEnv)
        {
            srcs = srcs.map(function(src){
                var srclst = src.split('https://formfacade.com');
                return srclst.length==2?(srclst.pop()+'?_='+new Date().getTime()):src;
            });
        }
        var prms = srcs.map(function(src){
            return new Promise(function(resolve, reject){
                curr.loadScript(src, resolve);
            });
        });
        return Promise.all(prms);
    }

    this.fetchScrape = function(publishId)
    {
        if(this.data.request.query.template == 'cloud')
            return fetch(`/uploaded/templates/${publishId}`).then(req=>req.json()).then(res=>res.scrape||{});
        else if(this.data.request.query.template == 'draft')
            return fetch(`https://formfacade-template-default-rtdb.firebaseio.com/scrape/${publishId}-${this.data.request.query.uid}.json`).then(req=>req.json()).then(jso=>jso||{});
        else
            return fetch('https://cache.formfacade.com/data/scrape/'+publishId).then(req=>req.json()).then(jso=>jso||{});
    }

    this.fetchPublish = function(publishId)
    {
        var {template, uid} = this.data.request.query;
        if(template == 'cloud')
            return fetch(`/uploaded/templates/${publishId}`).then(req=>req.json()).then(res=>res.publish||{});
        else if(template == 'draft')
            return fetch(`https://formfacade-template-default-rtdb.firebaseio.com/publish/${publishId}-${uid}.json`).then(req=>req.json()).then(jso=>jso||{})
            ;
        else
            return fetch('https://cache.formfacade.com/data/publish/'+publishId).then(req=>req.json()).then(jso=>jso||{})
            ;
    }

    this.fetchFacade = function(publishId)
    {
        var {template, uid} = this.data.request.query;
        if(template == 'cloud')
            return Promise.resolve({});
        else if(template == 'draft')
            return fetch(`https://formfacade-template-default-rtdb.firebaseio.com/facade/${publishId}-${uid}.json`).then(req=>req.json()).then(jso=>jso||{});
        else 
            return fetch('https://cache.formfacade.com/data/facade/'+publishId+'-editable').then(req=>req.json()).then(jso=>jso||{});
    }

    this.fetchInfo = function(userId)
    {
        return fetch('https://cache.formfacade.com/data/team/'+userId+'/info').then(req=>req.json()).then(jso=>jso||{});
    }

    this.fetchPaid = function(userId)
    {
        return fetch('https://cache.formfacade.com/data/user/'+userId+'/paid').then(req=>req.json()).then(jso=>{
            return Object.keys(jso || {}).length==0?null:jso;
        });
    }

    this.fetchPrefill = function(publishId, prefillId)
    {
        return fetch('https://cache.formfacade.com/data/prefill/'+publishId+'/link/'+prefillId).then(req=>req.json()).then(jso=>jso||{});
    }

    this.fetchContact = function(userId, publishId, contactId)
    {
        var baseurl = this.data.devEnv?'http://localhost:5000':'https://formfacade.com';
        return fetch(baseurl+'/contact/'+userId+'/form/'+publishId+'/'+contactId).then(req=>req.json()).then(jso=>jso||{});
    }

    this.fetchResponse = function(publishId, savedId)
    {
        var baseurl = this.data.devEnv?'http://localhost:5000':'https://formfacade.com';
        return fetch(baseurl+'/draft/'+publishId+'/read/'+savedId).then(req=>req.json()).then(jso=>jso||{});
    }

    this.fetchApprovers = function(publishId)
    {
        return fetch('https://cache.formfacade.com/data/personalize/'+publishId+'/approvers').then(req=>req.json()).then(jso=>jso||{});
    }

    this.init = function(savedId)
    {
        this.result = null;
        this.__sections = null;
        var {userId, publishId} = this.data.request.params;
        var prms = [
            this.fetchScrape(publishId), this.fetchPublish(publishId), this.fetchFacade(publishId),
            this.fetchInfo(userId), this.fetchPaid(userId)
        ];
        savedId = savedId||this.readCookie('ff-'+publishId);
        var {flush, restoreId, appearance, fulledit, copyId, prefillId, lang, officeuseSection, contactId, moveto} = this.data.request.query;
        var urlparams = new URLSearchParams(window.location.search);
        flush = flush||urlparams.get('ff-flush');
        restoreId = restoreId||urlparams.get('restoreId');
        fulledit = fulledit||urlparams.get('fulledit');
        appearance = appearance||urlparams.get('appearance');
        officeuseSection = officeuseSection||urlparams.get('officeuseSection');
        moveto = moveto||urlparams.get('moveto');
        if(moveto)
            this.data.moveto = moveto||null;
        if(restoreId || fulledit)
        {
            this.data.restoreId = restoreId||fulledit;
            this.data.fulledit = fulledit;
            this.data.appearance = appearance;
            this.data.officeuseSectionId = officeuseSection||null;
            savedId = restoreId||fulledit;
            flush = false;
        }
        copyId = copyId||urlparams.get('copyId');
        prefillId = prefillId||urlparams.get('prefillId');
        contactId = contactId||urlparams.get('contactId');
        if(prefillId)
        {
            var prefillprm = this.fetchPrefill(publishId, prefillId).then(jso=>jso.entry?{entry:jso.entry}:{});
            prms.push(prefillprm);
        }
        else if(copyId)
        {
            var copyprm = this.fetchResponse(publishId, copyId).then(jso=>jso.entry?{entry:jso.entry}:{});
            prms.push(copyprm);
        }
        else if(contactId)
        {
            var contactprm = this.fetchContact(userId, publishId, contactId).then(jso=>jso.entry?{entry:jso.entry}:{});
            prms.push(contactprm);
        }
        else if(savedId && !flush)
        {
            var savedprm = this.fetchResponse(publishId, savedId);
            prms.push(savedprm);
        }
        else
        {
            var savedprm = Promise.resolve({});
            prms.push(savedprm);
        }

        if(this.data.officeuseSectionId) {
            var approverprm = this.fetchApprovers(publishId);
            prms.push(approverprm);
        } else {
            var approverprm = Promise.resolve({});
            prms.push(approverprm);
        }

        var curr = this;
        if(window.posMode && curr.data.request.params.userId) {
            try {
                curr.posMode = new POS(curr.data.request.params.userId, { authentication: { userId: curr.data.request.params.userId } });
            } catch(error) {
                console.error(error);
            }
        }
        return Promise.all(prms).then(function(rs){
            var [scraped, form, facade, info, paid, drft, approvers] = rs;
            curr.data.scraped = curr.data.ban?{error:'Not public', errorMessage:curr.data.ban}:scraped;
            curr.data.form = form;
            curr.data.facade = facade;
            curr.data.approvers = approvers;
            curr.config = Object.assign(curr.config, info);
            curr.config.payment = paid||{};
            if(curr.isPaid(userId, {paid}))
            {
                curr.config.branded = paid.branded;
                curr.config.plan = 'paid';
            }
            if(drft.entry)
            {
                curr.draft = drft;
                if(!curr.draft.pageHistory) curr.draft.pageHistory = [];
                if(!curr.draft.activePage) curr.draft.activePage = 'root';
                if(restoreId) curr.draft.activePage = 'root';
            }
            else
                curr.draft = curr.prefill();
            var setting = curr.data.facade.setting||{};
            var ln = setting.language||info.language||lang;
            try {
                if(curr.posMode && window.ffFormLoaded && typeof window.ffFormLoaded == 'function') {
                    window.ffFormLoaded();
                }
            } catch (error) {
                console.error(error);
            }
            return curr.loadLanguage(ln);
        });
    }

    this.loadLanguage = function(ln)
    {
        if(ln && this.langtext && ln!=this.langtext.language)
        {
            var baseurl = this.data.devEnv?'http://localhost:5000':'https://formfacade.com';
            return fetch(baseurl+'/include/language/'+ln+'.json').then(req=>req.json()).then(langtext=>{
                this.langtext = langtext;
            });
        }
        else
        {
            return Promise.resolve();
        }
    }

    this.load = function(divId)
    {
        var curr = this;
        curr.divId = divId;
        var cfgstg = curr.config.setting||{};
        var trg = curr.data.request.params.target;
        if(trg=='bootstrap' || trg=='gsuite' || trg=='clean')
        {
            curr.addLinkTag('/css/open-props.min.css');
            curr.addLinkTag('/css/formfacade.css');
            curr.addLinkTag('/css/formfacade.boot.css');
            if(cfgstg.currency) curr.addLinkTag('/css/neartail.css');
        }
        else
        {
            curr.addLinkTag('/css/tailwind/output.css');
        }
        var celm = curr.getContentElement();
        if(celm)
        {
            celm.innerHTML = curr.template.preview;
            var frm = celm.querySelector('form');
            if(frm) frm.addEventListener('submit', function(event){
                event.preventDefault();
                return false;
            });
        }
        this.init().then(function(){
            if(!curr.getContentElement()) {
                console.warn('Warn: No content element found');
                if(window.isFormBuilder) {
                    curr.load(curr.divId)
                }
                return;
            }
            var fac = curr.data.facade||{};
            var facstg = fac.setting||{};
            if(!cfgstg.currency && facstg.currency)
                curr.addLinkTag('/css/neartail.css');
            if(curr.isEditMode())
            {
                if(curr.data.scraped.items)
                    curr.render();
                else if(curr.hasCreator())
                    curr.getContentElement().innerHTML = curr.template.preview;
                else
                    curr.render();
                curr.addLinkTag('/css/formfacade.editor.css');
                curr.addLinkTag('/css/neartail.editor.css');
            }
            else
            {
                curr.render();
            }
            var urlparams = new URLSearchParams(window.location.search);
            var addtocart = urlparams.get('addtocart');
            if(addtocart)
            {
                var cartitm = curr.data.scraped.items[addtocart]||{};
                var cartsec = cartitm.section||{};
                if(cartsec.id!='root') curr.directtoSection(cartsec.id);
                curr.showProduct(addtocart, 2);
                var cartelm = document.getElementById('ff-id-'+addtocart);
                curr.scrollIntoView(cartelm);
            }
            if(window.cartSidebar) cartSidebar.fetch('load');
            if(window.facadeListener) facadeListener.onChange('load', curr);
            var callback = curr.data.request.query.callback;
            if(callback && window[callback])
                window[callback](curr);
            curr.scrapeSection();
            if(!window.Stripe && curr.getPaymentButtons().length>0)
                curr.loadScript('https://js.stripe.com/v3/', function(){ });

            curr.checkOneTapLogin();
        });
    }

    this.closeAuthenticatePopup = function()
    {
        var popup = document.getElementById('force-popup');
        if(popup) popup.remove();
    }

    this.signInWithGoogle = async () => 
    {
        var curr = this;
        const saveBtn = document.getElementById('popup-signin') || {};
        if(saveBtn)
        {
            saveBtn.innerHTML = 'Signing In...';
            saveBtn?.classList?.add('cursor-loading');
            saveBtn.disabled = true;
        }
        var provider = new firebase.auth.GoogleAuthProvider();
        provider.setCustomParameters({
            prompt: 'select_account'
        });
        return await window.neartlFbInstance.auth().signInWithPopup(provider).then((result) => {
            formFacade.posMode.authenticationToken = result.credential.accessToken;
            setTimeout(() => {
                window.location.reload();
            }, 3000);
        }).catch(async (err) => {
            console.warn('Error in Line sidebar.signInWithPopup');
            console.error(err);
            return;
        });
    }

    this.showAuthenticatePopup = function(closeable = false)
    {
        let html = `
            <div class="relative z-10" aria-labelledby="modal-title" role="dialog" aria-modal="true">
                <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"></div>
                <div class="fixed inset-0 z-10 overflow-y-auto">
                    <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
                    <div class="relative transform overflow-hidden rounded-lg bg-white px-4 pb-4 pt-5 text-left shadow-xl transition-all sm:my-8 sm:max-w-lg sm:p-6 m-auto h-max">
                        <div>
                        <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-purple-100">
                            <img class="h-10 w-10" src="https://near.tl/logo.png" alt="Login to continue to POS Mode.">
                        </div>
                        <div class="mt-3 text-center sm:mt-5">
                            <h3 class="text-lg font-semibold leading-6 text-gray-900" id="modal-title">
                                To proceed, you need to be logged in.
                            </h3>
                            <p id="login-description" class="text-sm text-gray-500 mt-3"></p>
                        </div>
                        </div>
                        <div class="mt-5 sm:mt-6 flex flex-row items-center justify-between gap-3">
                        ${closeable ?
                `<button id="popup-signin-cancel" type="button" class="mt-3 inline-flex w-full justify-center rounded-md px-3 py-2 text-sm font-semibold text-white shadow-sm ring-1 ring-inset ring-gray-300  sm:col-start-1 sm:mt-0 bg-red-500 hover:bg-red-700" onclick="formFacade.closeAuthenticatePopup()">
                            Cancel
                            </button>`
                : ''
            }
                        <button id="popup-signin" type="button" class="mt-3 inline-flex w-full justify-center rounded-md px-3 py-2 text-sm font-semibold text-white shadow-sm ring-1 ring-inset ring-gray-300  sm:col-start-1 sm:mt-0 bg-blue-500 hover:bg-blue-700" onclick="formFacade.signInWithGoogle()">
                            Login
                        </button>
                        </div>
                    </div>
                    </div>
                </div>
            </div>
        `;
        var div = document.createElement('div');
        div.setAttribute('id', 'force-popup');
        div.innerHTML = html;
        document.body.appendChild(div);
    }

    this.addLinkTag = function(lnkurl)
    {
        var exlnk = document.getElementById(lnkurl);
        if(exlnk) return;
        var host = this.data.devEnv?'':'https://formfacade.com';
        var version = this.data.devEnv?new Date().getTime():114;
        var lnkfull = host+lnkurl+'?nocache='+version;
        var relm = document.getElementsByTagName('head')[0]||document.getElementsByTagName('body')[0];
        var link = document.createElement('link');
        link.id = lnkurl;
        link.rel = 'stylesheet';
        link.href = lnkfull;
        relm.appendChild(link);
    }

    this.createCookie = function(name, value, days) 
    {
        if (days) {
            var date = new Date();
            date.setTime(date.getTime()+(days*24*60*60*1000));
            var expires = "; expires="+date.toGMTString();
        }
        else var expires = "";
        document.cookie = name+"="+value+expires+"; path=/";
    }

    this.readCookie = function(k)
    {
        var val = (document.cookie.match('(^|; )'+k+'=([^;]*)')||0)[2];
        return val&&val.trim()==""?null:val;
    }

    this.hasCreator = function()
    {
        var fac = this.data.facade||{};
        var setting = fac.setting||{};
        var form = this.data.form||{};
        var editors = form.editors||[];
        return editors.indexOf('creator@neartail.com')>=0 || setting.creator=='neartail' 
        || (setting.currencyCode && editors.indexOf('editor@formfacade.com')>=0);
    }

    this.hasEditor = function()
    {
        var editors = this.data && this.data.form && this.data.form.editors;
        return editors && editors.indexOf('editor@formfacade.com')>=0;
    }

    this.hasCreatorOrEditor = function() {
        return this.hasCreator() || this.hasEditor();
    }

    this.isEditMode = function()
    {
        return location.href.indexOf('/editor/form/')>=0 || location.href.indexOf('/formbuilder/form/')>=0 || location.href.indexOf('/oldeditor/form/')>=0;
    }

    this.removeCustomCSSInEditor = function() {
        if(this.isEditMode() && window.isFormBuilder) {
            return true;
        }
        return false;
    }

    this.isPreviewMode = function()
    {
        if(window.editFacade)
            return true;
        else if(location.href.indexOf('https://formfacade.com/edit/')==0)
            return true;
        else if(location.href.indexOf('https://formfacade.com/embed/')==0)
            return true;
        else if(location.href.indexOf('https://formfacade.com/share/')==0)
            return true;
        return false
    }

    this.launchPreview = function()
    {
        var msg = 'You are in edit mode. Do you want to test this form in preview mode?';
        if(confirm(msg)) window.open(location.href.replace('/editor/','/preview/'));
    }

    this.html = function(txt)
    {
        if(txt)
        {
            txt = txt.trim().replace(/(?:\r\n|\r|\n)/g, '<br>');

            replacePattern1 = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
            txt = txt.replace(replacePattern1, '<a href="$1" target="_blank">$1</a>');

            replacePattern2 = /(^|[^\/])(www\.[\S]+(\b|$))/gim;
            txt = txt.replace(replacePattern2, '$1<a href="http://$2" target="_blank">$2</a>');

            replacePattern3 = /(([a-zA-Z0-9\-\_\.])+@[a-zA-Z\_]+?(\.[a-zA-Z]{2,6})+)/gim;
            txt = txt.replace(replacePattern3, '<a href="mailto:$1">$1</a>');
        }
        return txt;
    }

    this.val = function(title)
    {
        var items = this.data.scraped.items||{};
        for(var i in items)
        {
            var item = items[i];
            if(this.draft && item.title==title)
                return this.draft.entry[item.entry];
        }
    }

    this.entry = function(entryId)
    {
        if(this.draft.entry)
        {
            var entryval = this.draft.entry[entryId];
            if(entryval)
                return entryval;
            else
                this.draft.entry[entryId.toString()];
        }
    }

    this.getContentElement = function()
    {
        var elm = document.querySelector(this.divId);
        return elm;
    }

    this.getPhone = function(pg='root')
    {
        if(this.data.facade && this.data.facade.whatsapp)
        {
            var itms = this.data.scraped.items||{};
            var entrs = this.draft.entry||{};
            var ph = this.data.facade.whatsapp.phone;
            var sbmts = this.data.facade.submit||{};
            if(sbmts[pg])
            {
                var sbmt = sbmts[pg];
                var itm = itms[sbmt.router]||{};
                if(sbmt.submitto)
                {
                    if(sbmt.submitto=='whatsapp')
                    {
                        ph = sbmt.waphone||ph;
                        return entrs[itm.entry]||ph;
                    }
                    else
                        return null;
                }
                else
                {
                    return entrs[itm.entry]||ph;
                }
            }
            return ph;
        }
    }

    this.showMessage = function(secid)
    {
        var curr = this;
        var doc = this.getDocument();
        if(this.getPhone(secid))
        {
            var elms = doc.querySelectorAll('#ff-submit-'+secid);
            elms.forEach(function(elm){
                elm.innerHTML = curr.lang('Launching WhatsApp...');
            });
        }
        else
        {
            var elms = doc.querySelectorAll('#ff-submit-'+secid+' img');
            elms.forEach(function(elm){ 
                elm.src = 'https://formfacade.com/img/loading.svg'; 
            });
        }
    }

    this.clearSignature = function(itemId, entryId) {
        this.signaturePad[itemId].clear();
        var input = document.getElementById(`Widget${itemId}`);
        if(input) input.value = "";
        this.draft.entry[entryId] = "";
        this.saveDraft();
    }

    this.renderSignature = function()
    {
        var curr = this;
        if(typeof(SignaturePad)!='function') return;
        var signatures = document.querySelectorAll('.ff-signature');
        var params = curr.data.request.params;
        var publishId = params.publishId;

        signatures.forEach(function(signature){
            var ds = signature.dataset;
            var itemId = ds.id;
            var entryId = ds.entry;
            var canvas =  signature.querySelector('canvas')
            var imageUrl;

            if(curr.draft && curr.draft.entry && curr.draft.entry[entryId])
                imageUrl = curr.draft.entry[entryId]

            if(!signature.dataset.esignAdded && canvas){
                canvas.width = signature.clientWidth;
                curr.signaturePad[itemId] = new SignaturePad(canvas, { itemId: itemId});
                var signaturePad = curr.signaturePad[itemId];
                signaturePad.addEventListener("endStroke", (event) => {
                    var savedId = curr.draft.savedId;
                    var signUrl = `https://formfacade.com/uploaded/${publishId}/${savedId}/${entryId}/esign.png`
                    var input = document.getElementById(`Widget${itemId}`);
                    input.value = signUrl;
                    curr.draft.entry[entryId] = signUrl;
                    curr.signatures.signs[entryId] = curr.signaturePad[itemId].toDataURL()
                    curr.saveSignImage(itemId, entryId);

                    if(!params.publishId) {
                        var data = {publishId: publishId, currParams: curr.data.request.params, params: params, userAgent: navigator.userAgent}
                        curr.showWarning('Signature undefined issue', data, null, {ignorePopup: true});
                    }
                });

                if(signaturePad && signaturePad.isEmpty() && curr.signatures.signs[entryId]) {
                    signaturePad.fromDataURL(curr.signatures.signs[entryId], { width: canvas.clientWidth, height: canvas.clientHeight })
                }
                signature.dataset.esignAdded = true;
            }
        });
    }


    this.saveSignImage = function(itemId, entryId) {
        var curr = this;
        var baseurl = 'https://formfacade.com';
        if(curr.data.devEnv)
            baseurl = 'http://localhost:5000';
        var publishId = curr.data.request.params.publishId;

        var savedId = curr.draft.savedId;
        if(!savedId) savedId = curr.readCookie('ff-'+publishId);
        var prm = Promise.resolve(savedId);
        if(!savedId) prm = curr.saveDraft().then(_=>curr.draft.savedId);
        curr.saving = prm.then(function(svid){
            if(!svid) throw Error('Save failed! Try again.');
            var data = { image: curr.signaturePad[itemId].toDataURL() };
            return fetch(baseurl+'/signature/'+publishId+'/'+svid+'/'+entryId, {
                method: 'post',
                headers: { accept: 'application/json', 'content-type': 'application/json' },
                body: JSON.stringify(data),
            }).then(function(response){
                return response.json(); 
            }).then(function(response){
                var input = document.getElementById(`Widget${itemId}`);
                if(response && response.url && input) {
                    curr.draft.entry[entryId] = response.url;
                    input.value = response.url;
                }
                return curr.saveDraft();
            });
        });

        return curr.saving;

    }

    this.debounce = function(func, delay) {
        var curr = this;      
        
        return function() {
          const context = this;
          const args = arguments;
          clearTimeout(curr.debounceTimer);

          curr.debounceTimer = setTimeout(() => {
            func.apply(context, args);
          }, delay || 500);
        };
    }

    this.renderUpload = function(locale)
    {
        if(!window.Uppy) return;
        var curr = this;
        if(!this.data.locale) this.data.locale = locale;
        var uploads = this.getDocument().querySelectorAll('.ff-file-upload');
        uploads.forEach(function(upload){
            if(!upload.dataset.uppied)
            {
                var ds = upload.dataset;
                var filearr = [];
                if(ds.files) filearr = ds.files.split(',');
                filearr = filearr.map(function(fl){ return fl.trim() });
                curr.renderUploadField(ds.id, ds.entry, filearr);
                upload.dataset.uppied = true;
            }
        });
    }

    this.extensions = 'pdf, doc, docx, xls, xlsx, ppt, pptx, csv, txt, rtf, html, zip, mp3, wma, mpg, flv, avi, 3gp, m4v, mov, mp4, wmv, jpg, jpeg, png, gif';

    this.renderUploadField = function(id, entry, files)
    {
        var curr = this;
        var baseurl = 'https://formfacade.com';
        if(curr.data.devEnv)
            baseurl = 'http://localhost:5000';
        var publishId = curr.data.request.params.publishId;
        var itm;
        if(curr.data.scraped.items)
            itm = curr.data.scraped.items[id];
        if(!itm) itm = {};
        var fcitm;
        if(curr.data.facade.items)
            fcitm = curr.data.facade.items[id];
        if(!fcitm) fcitm = {};
        var maxnum = fcitm.maxnum?fcitm.maxnum:1;
        var minnum = itm.required?1:0;
        var filemb = curr.config.filemb||10;
        var mbtxt = filemb<1000?(filemb+' MB max'):(filemb/1000+' GB max');
        var ph = (minnum==maxnum?maxnum:(minnum+'-'+maxnum))+' file'+(maxnum==1?'':'s')+', '+mbtxt;
        var uppyopts = {
            debug:true, autoProceed:true,
            restrictions: {maxFileSize:filemb*1024*1024, maxNumberOfFiles:maxnum, minNumberOfFiles:minnum},
            onBeforeFileAdded: function (currentFile, files) {
                if(curr.isIos()) {
                    if(files) {
                        var uploadedFileNames = [];
                        for(let uploadedFile in files) {
                            uploadedFileNames.push(files[uploadedFile].name)
                        }
                        if(uploadedFileNames.indexOf(currentFile.name) > -1) {
                            var fileName = currentFile.name.split('.');
                            var extension = fileName.pop();
                            fileName = fileName.join('.')
                            currentFile.name = `${fileName}_${Date.now()}.${extension}`
                        }
                    }
                }
                return currentFile;
            }
        };
        if(this.data.locale)
            uppyopts.locale = Uppy.locales[this.data.locale]||Uppy.locales.en_US;
        var exts = fcitm.extension?fcitm.extension:this.extensions;
        if(exts!='all')
            uppyopts.restrictions.allowedFileTypes = exts.split(',').map(function(ext){ return '.'+ext.trim(); });
        var uppy = new Uppy.Uppy(uppyopts).use(Uppy.Dashboard, {
            trigger:'#Display'+id, note:ph,
            showProgressDetails:true, 
            showRemoveButtonAfterComplete:true,
            browserBackButtonClose:true, proudlyDisplayPoweredByUppy:false, 
            doneButtonHandler: function() {
                if(uppy && uppy.getPlugin('Dashboard')) uppy.getPlugin('Dashboard').closeModal();
            }
        })
        .use(Uppy.AwsS3, {
            limit:1, timeout:1000*60*60,
            getUploadParameters(file) {
                var savedId = curr.draft.savedId;
                if(!savedId) savedId = curr.readCookie('ff-'+publishId);
                var prm = Promise.resolve(savedId);
                if(!savedId) prm = curr.saveDraft().then(_=>curr.draft.savedId);
                return prm.then(function(svid){
                    if(!svid) throw Error('Save failed! Try again.');
                    return fetch(baseurl+'/signedurl/'+publishId+'/'+svid+'/'+entry, {
                        method: 'post',
                        headers: {accept: 'application/json', 'content-type': 'application/json',},
                        body: JSON.stringify({filename: file.name, contentType: file.type}),
                    }).then(function(response){
                        return response.json(); 
                    });
                });
            }
        });
        var updateFiles = function()
        {
            var uploads = uppy.getFiles().map(function(up){
                var savedId = curr.draft.savedId;
                if(!savedId) savedId = curr.readCookie('ff-'+publishId);
                var flname = up.uploadURL.split('%2F').pop();
                var flurl = 'https://formfacade.com/uploaded/'+publishId+'/'+savedId+'/'+entry+'/'+flname;
                return flurl;
            });
            var wdg = curr.getDocument().getElementById('Widget'+id);
            if(wdg) wdg.value = uploads.join(', ');
            curr.draft.entry[entry] = uploads.join(', ');
            curr.saveDraft();
        }
        uppy.on('complete', function(result){
            if(result.successful) updateFiles();
            var donebtns = curr.getDocument().querySelectorAll('.uppy-StatusBar-content[title="Complete"] .uppy-StatusBar-statusPrimary');
            donebtns.forEach(function(donebtn){
                donebtn.addEventListener('click', function(){ uppy.getPlugin('Dashboard').closeModal(); });
            });
        });
        uppy.on('file-removed', function(file, reason){
            updateFiles();
        });
    }

    this.saveDraft = function(evt)
    {
        var curr = this;
        curr.cachedBill = null;
        if(curr.saving && !curr.draft.savedId) return curr.compute();
        curr.saving = new Promise(function(resolve, reject){
            var elm = curr.getContentElement();
            if(!elm) return;
            var frm = elm.querySelector('form');
            if(!frm) return;
            var formData = new FormData(frm);
            if(!formData.entries) return;
            var entries = formData.entries();
            var variants = {};
            var pairs = {};
            var next, entry;
            while ((next = entries.next()) && next.done === false) 
            {
                entry = next.value;
                var [ename, evalue] = entry;
                if(ename=='emailAddress' && evalue)
                {
                    if(!formFacade.draft) formFacade.draft = {};
                    formFacade.draft.emailAddress = evalue;
                }
                else if(ename=='responseId' && evalue)
                {
                    if(!formFacade.draft) formFacade.draft = {};
                    formFacade.draft.responseId = evalue;
                }
                else if(ename.indexOf('variant.')==0 && evalue)
                {
                    var [vprefix, ventry] = ename.split('.');
                    var vrn = variants[ventry]||[];
                    vrn.push(evalue);
                    variants[ventry] = vrn;
                }
                else if(ename.indexOf('entry.')==0)
                {
                    var nms = ename.split('entry.');
                    var nm = nms.pop();
                    nm = nm.replace('.','-');
                    var val = pairs[nm];
                    if(!nm)
                    {
                        console.warn('Invalid parameter', next, val);
                    }
                    else if(val)
                    {
                        var valarr = Array.isArray(val)?val:[val];
                        valarr.push(evalue);
                        pairs[nm] = valarr;
                    }
                    else if(evalue)
                    {
                        pairs[nm] = evalue;
                    }
                }
            }
            var fac = curr.data.facade||{};
            var enhance = fac.enhance||{};
            if(!enhance.layout || enhance.layout=='default')
            {
                var txtareas = elm.querySelectorAll('.ff-item-prd textarea');
                txtareas.forEach(txtarea=>{
                    var txtname = txtarea.name||'';
                    var [txtprefix, txtentry] = txtname.split('.');
                    if(txtprefix=='entry')
                    {
                        var vrnlist = variants[txtentry];
                        var vrntxt = vrnlist?vrnlist.join('\n'):'';
                        txtarea.value = vrntxt;
                        pairs[txtentry] = vrntxt;
                    }
                });
            }
            formFacade.draft.entry = pairs;
            var mapping = fac.mapping||{};
            var sitems = curr.data.scraped.items||{};
            ['name', 'email', 'phone'].forEach(attr=>{
                var iid = mapping[attr];
                var itm = sitems[iid]||{};
                var enval = pairs[itm.entry];
                if(itm.entry && enval) formFacade.draft[`map-${attr}`] = enval;
            });
            var http = new XMLHttpRequest();
            var baseurl = 'https://formfacade.com';
            if(curr.data.devEnv)
                baseurl = 'http://localhost:5000';
            var publishId = curr.data.request.params.publishId;
            var httpurl = baseurl+'/draft/'+publishId+'/save';
            var userId = curr.data.request.params.userId;
            if(userId) httpurl = baseurl+'/draft/'+userId+'/form/'+publishId+'/save';
            http.open('POST', httpurl, true);
            http.setRequestHeader('Content-type', 'application/json; charset=UTF-8');
            http.responseType = 'json';
            http.onload = function()
            {
                var jso = http.response;
                if(jso.savedId)
                {
                    curr.draft.savedId = jso.savedId;
                    if(jso.draftSeq) curr.draft.draftSeq = jso.draftSeq;
                    curr.createCookie('ff-'+publishId, jso.savedId, 3/24);
                    var evtname = evt&&evt.target&&evt.target.name?evt.target.name:'visit';
                    curr.stat(evtname);
                }
                resolve(jso);
            }
            http.onerror = err=>reject(http.response||"Couldn't connect to server");
            curr.draft.originTime = formFacade.config.originTime;
            curr.draft.originId = formFacade.config.originId;
            if(!curr.draft.savedId)
            {
                curr.draft.lead = {
                    page:location.href||null, prev:document.referrer||null, agent:navigator.userAgent||null,
                    platform:navigator.platform||null, width:screen.width||null, height:screen.height||null,
                    language:navigator.language||null, timeZone:Intl.DateTimeFormat().resolvedOptions().timeZone||null
                };
                var utmparams = new URLSearchParams(window.location.search);
                ['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content'].forEach(utmnm=>{
                    var utmvl = utmparams.get(utmnm);
                    if(utmvl) curr.draft.lead[utmnm] = utmvl;
                });
            }
            http.send(JSON.stringify(formFacade.draft));
            if(evt && evt.target && evt.target.name)
            {
                var entrg = evt.target.name.split('entry.').pop();
                var scr = curr.data.scraped;
                var fcd = curr.data.facade;
                for(var iid in scr.items)
                {
                    var itm = scr.items[iid];
                    var fitm = fcd&&fcd.items?fcd.items[iid]:null;
                    if(itm.entry==entrg && fitm && fitm.js)
                    {
                        try{
                            eval(fitm.js);
                        }
                        catch(err){
                            console.error(fitm.js+' failed with '+err);
                        }
                    }
                }
            }
            curr.compute();
            if(window.cartSidebar) cartSidebar.fetch('save');
            if(window.facadeListener) facadeListener.onChange('save', curr);
        }).then(function(){
            curr.saving = null;
            return;
        }).catch(function(err){
            console.warn('Save failed: '+err);
            curr.saving = null;
            return;
        });
        return curr.saving;
    }

    this.render = function()
    {
        var curr = this;
        var styelm = this.getDocument().getElementById('ff-style-header');
        if(this.isEditMode())
        {
            if(styelm) styelm.parentNode.removeChild(styelm);
            styelm = null;
        }
        if(!styelm)
        {
            styelm = document.createElement('div')
            styelm.id = 'ff-style-header';
            var bodyelm = document.getElementsByTagName('body')[0];
            if(bodyelm) bodyelm.appendChild(styelm);
            styelm.innerHTML = ejs.render(this.template.style, this);
        }
        var elm = this.getContentElement();
    	if(!elm) return;
        if(!this.__compiledtext) this.__compiledtext = ejs.compile(this.template.text);
        elm.innerHTML = this.__compiledtext(this);
        this.renderUpload();
        this.renderSignature();
        
        var pypanes = elm.querySelectorAll('.walletpane');
        if(pypanes.length>0)
        {
            var peerhost = curr.data.devEnv?'//localhost:3000':'//pay.peergateway.com';
            window.loadingScripts = window.loadingScripts||curr.loadScripts([peerhost+'/js/pay/google-forms.js?_=v20']);
            var fac = curr.data.facade||{};
            var stg = fac.setting||{};
            window.loadingScripts.then(_=>{
                pypanes.forEach(pyp=>{
                    var {wallet, to, amount} = pyp.dataset||{};
                    var note = (curr.draft.savedId||'Neartail').replace(/[^A-Z0-9]/ig, '');
                    if(curr.draft.submitSeq)
                    {
                        var number = curr.draft.submitSeq;
                        var ttl = curr.data.scraped.form||curr.data.scraped.title||'Order';
                        ttl = ttl.replace(/[^A-Z0-9]/ig, '').toUpperCase();
                        ttl = ttl.length>3?ttl.substring(0,3):'ORD';
                        if (number.toString().length > 5) note = number;
                        let s = '000000' + number;
                        note = ttl + s.substr(s.length - 5);
                    }
                    var opts = {
                        app:'neartail', userId:curr.data.request.params.userId, wallet:wallet, 
                        orderId:curr.draft.savedId, amount:amount, currency:stg.currency, currencyCode:stg.currencyCode,
                        to:to, toname:curr.config.title||curr.data.scraped.title, note:note
                    };
                    pyp.payment = new GoogleFormsPayment(opts);
                    pyp.payment.load('#'+pyp.id);
                });
            });
        }

        curr.compute();
        var frm = elm.querySelector('form');
        if(!frm) return console.warn('Form not found in Formfacade');
        frm.addEventListener('change', function(evt){
            curr.saveDraft(evt);
        });

        var config = this.config||{};
        if(config.plan=='warned' || config.plan=='blocked')
        {
            var fac = this.data.facade||{};
            var facstg = fac.setting||{};
            var params = this.data.request.params||{};
            var pricingpage = 'https://formfacade.com/website/pricing.html';
            if(facstg.currency) pricingpage = 'https://neartail.com/order-form/pricing.html';
            this.showPopup(
                `${config.plan=='blocked'?'âš¡':'âš '} Free limit exceeded`,
                'This form has exceeded its free limit. If you are the owner of this form, please upgrade to a paid plan. If not, contact the owner.',
                `<a class="btn btn-lg btn-primary" href="${pricingpage}?userId=${params.userId}" target="_blank">Upgrade</a>`,
                {render:config.plan=='blocked'}
            );
        }
        var onload = curr.data.request.query.onload;
        if(onload && window[onload])
            window[onload](curr);
        var fc = curr.data.facade;
        var jsrender = fc&&fc.enhance?fc.enhance.js:null;
        if(jsrender) eval(jsrender);
        if((window && window.isFormBuilder) || curr.isEditMode())
        {
            if(curr && curr.draft && curr.draft.activePage && curr.draft.activePage != 'root' && editFacade.facade && editFacade.facade && editFacade.facade.setting && editFacade.facade.setting.progressBar === 'on')
            {
                curr.updateProgressBar(curr.draft.activePage);
            }
        }
        else if(curr && curr.draft && curr.draft.activePage && curr.draft.activePage != 'root' && curr.data && curr.data.setting && curr.data.setting.progressBar === 'on')
        {
            curr.updateProgressBar(curr.draft.activePage);
        }
    }

    this.stat = function(evtname)
    {
    }

    this.showAll = function()
    {
        var doc = this.getDocument();
        doc.querySelectorAll('.ff-section').forEach(function(sec){ sec.style.display = 'block'; });
    }

    this.getCartItems = function()
    {
        var curr = this;
        var fac = curr.data.facade;
        var prds = curr.data.scraped.items;
        var oitems = fac.items;
        var crncy = fac.setting.currency;
        if (!crncy) {
            return [];
        }
        var billfn = "${getBill('" + crncy + "')}";
        var lines = curr.calculateEngine(billfn, { returntype: true });
        return lines.map(itm => {
            var oitem;
            [ttl, prc, qnt, itmid, ent, disc] = itm;
            for (var iid in prds) {
                var prd = prds[iid];
                if (prd.entry == ent) {
                    oitem = oitems ? oitems[iid] : null;
                }
            }
            if (oitem && oitem.measure === 'Nested' && itm && itm.length > 2) {
                var vid = itm[itm?.length - 2];
                var v1id = itm[itm?.length - 1];
                var qty = itm[2];
                return {
                    id: itmid,
                    entryId: ent,
                    vid: vid,
                    v1id: v1id,
                    qty: qty,
                    title: ttl
                }
            }
            return {
                id: itmid,
                entryId: ent,
                qty: qnt,
                vid: itm[itm?.length - 1],
                title: ttl
            }
        });
    }

    this.checkInventoryForCartItems = function(secid, cartItems)
    {
        if(!cartItems || cartItems.length === 0) return Promise.resolve({ itemsOutOfStock: [] });
        var curr = this;
        var { publishId } = curr.data.request.params;
        curr.showMessage(secid);
        return fetch(`https://cache.formfacade.com/data/facade/${publishId}-editable/items`).then(res => res.json()).then(citems => {
            var itemsOutOfStock = [];
            cartItems.forEach(itm => {
                var iid = itm.id;
                var chitm = citems[iid] || {};
                if (chitm && chitm.measure === 'Nested') {
                    var variantConfig = chitm.variantConfig || {};
                    var inventory = variantConfig.inventory || {};
                    var inv = inventory[itm.vid + '-' + itm.v1id] || {};
                    var variants = chitm.variants || {};
                    var variants1 = chitm.variants1 || {};
                    var v = variants[itm.vid];
                    var v1 = variants1[itm.v1id];
                    var remaining = inv.remain;
                    if (!v || !v1) {
                        remaining = 0;
                    }
                    if (remaining === undefined && v && v1) remaining = "";
                    if (chitm.nested === 'Appointment' && v && v1) {
                        var ds = v.name + " " + v1.name;
                        var dt = curr.getDateObject(ds).getTime();
                        var today = new Date().getTime();
                        if (dt < today) {
                            remaining = 0;
                        }
                    }
                    if (remaining && typeof remaining === 'string') remaining = Number(remaining);
                    if (remaining !== "" && remaining < itm.qty) {
                        itemsOutOfStock.push({
                            id: itm.id,
                            entry: itm.entryId,
                            vid: itm.vid,
                            v1id: itm.v1id,
                            _: itm.vid + '-' + itm.v1id,
                            appointment: chitm.nested === 'Appointment',
                            title: itm.title,
                            error: {
                                title: chitm.nested === 'Appointment' ? `âš  ${itm.title} ${curr.lang("is not available")}`: remaining < itm.qty ? `${curr.lang("Insufficient Quantity for")} ${itm.title}` :`${itm.title} ${curr.lang("is not available")}`,
                                message: chitm.nested === 'Appointment' ? curr.lang("Sorry, other users have already booked this option, please choose another option.") : remaining < itm.qty ? curr.lang("The quantity you selected for the product exceeds the available stock") : curr.lang("Sorry, other users have already purchased this item, please choose another item.")
                            }
                        });
                    }
                }
                else if (chitm && chitm.inventory === 'yes') {
                    var measure = chitm.measure;
                    if (measure === 'Configurable') {
                        var variants = chitm.variants || {};
                        var remaining = variants[itm.vid].remain;
                        if (remaining === undefined) remaining = "";

                        if (remaining && typeof remaining === 'string') remaining = Number(remaining);

                        if (remaining !== "" && remaining < itm.qty) {
                            itemsOutOfStock.push({
                                id: itm.id,
                                entry: itm.entryId,
                                vid: itm.vid,
                                _: itm.vid,
                                title: itm.title,
                                error: {
                                    title: remaining < itm.qty ? `${curr.lang("Insufficient Quantity for")} ${itm.title}` :`${itm.title} ${curr.lang("is not available")}`,
                                    message: remaining < itm.qty ? curr.lang("The quantity you selected for the product exceeds the available stock") : curr.lang("Sorry, other users have already purchased this item, please choose another item.")
                                }
                            });
                        }
                    }
                    else {
                        var remaining = chitm.remain;
                        console.log(remaining, 'This is remaining');
                        if (remaining === undefined) remaining = "";
                        if (remaining && typeof remaining === 'string') remaining = Number(remaining);
                        if (remaining < itm.qty) {
                            itemsOutOfStock.push({
                                id: itm.id,
                                entry: itm.entryId,
                                _: itm.entryId,
                                title: itm.title,
                                error: {
                                    title: remaining < itm.qty ? `${curr.lang("Insufficient Quantity for")} ${itm.title}` :`${itm.title} ${curr.lang("is not available")}`,
                                    message: remaining < itm.qty ? curr.lang("The quantity you selected for the product exceeds the available stock") : curr.lang("Sorry, other users have already purchased this item, please choose another item.")
                                }
                            });
                        }
                    }
                }
            });
            return { itemsOutOfStock: itemsOutOfStock };
        });
    }

    this.showWarningForOutOfStockItems = function(secid, outOfStockItem)
    {
        if (!outOfStockItem) return;
        var curr = this;
        var doc = curr.getDocument();
        var onClickEdit = `cartSidebar.navigate('${outOfStockItem.entry}');`;
        var onClickRemove = `cartSidebar.navigate('${outOfStockItem.entry}');`;

        if (outOfStockItem.appointment) {
            onClickEdit = `cartSidebar.navigate('${outOfStockItem.entry}', {
                vid: '${outOfStockItem.vid}',
                v1id: '${outOfStockItem.v1id}',
                qty: 1
            });`;

            onClickRemove = `cartSidebar.navigate('${outOfStockItem.entry}', {
                vid: '${outOfStockItem.vid}',
                v1id: '${outOfStockItem.v1id}',
                qty: 1
            });`;
        }
        if (outOfStockItem._ === outOfStockItem.entry) {
            onClickRemove += `setTimeout(() => {formFacade.updateProduct('${outOfStockItem._}', null, true);}, 250)`;
        } else {
            onClickRemove += `setTimeout(() => {formFacade.updateQuantity('${outOfStockItem._}', null, true);}, 250)`;
        }
        var msg = `<div class='ff-out-of-stock-items'><p class='ff-item-qs'>${outOfStockItem.error.message}</p></div>`;

        // Change the submit button loader svg back to send
        var elms = doc.querySelectorAll('#ff-submit-' + secid + ' img');
        document.querySelectorAll('#pg .pg-payment-button img').forEach(function (elm) {
            elm.src = 'https://formfacade.com/img/send.svg';
        });
        elms.forEach(elm => {
            elm.src = "https://formfacade.com/img/send.svg"
        });

        return curr.showPopup(
            `<span style="color:red">${outOfStockItem.error.title}</span>`,
            msg,
            `<div class="prdfooter" style="margin-top:-16px !important;"> <a tabindex="0" href="javascript:void(0);" onclick="${onClickEdit}">${curr.lang('Edit')}</a> <a tabindex="0" href="javascript:void(0);" onclick="${onClickRemove}" class="prddel">${curr.lang('Remove')}</a> </div>`,
            { render: false }
        );
    }

    this.submitWithInventory = function (frm, secid, paymentPromise = null) 
    {
        // if iframe then open the form in new tab.
        if(window.top !== window.self && window.location)
        {
            // open new tab.
            var url = window.location.href || "";
            var isPreviewMode = url.indexOf('ff-mode=preview') > -1;
            // remove all the query params
            if(url.indexOf('?') > -1) url = url.split('?')[0];
            var fc = this.data.facade || {};
            var fcsub = fc.submit || {};
            var itmsubmit = fcsub[secid] || {};
            var submitto = itmsubmit.submitto || '';
            if (submitto === 'whatsapp' || (!submitto && fc.whatsapp && fc.whatsapp.phone)) 
            {
                // url += '?savedId=' + this.draft.savedId;
                var title = "WhatsApp Submission Unavailable in Iframe";
                var message = "Please open the form in a new tab to submit.";
                var footer = `<div class="prdfooter"><a href="${url}" target="_blank">Proceed</a></div>`;
                if (isPreviewMode) {
                    title = "WhatsApp Submission Unavailable in Preview Mode";
                    message = "WhatsApp form submissions are not supported in preview mode. Open the form in a new tab to proceed.";
                }
                if (paymentPromise && secid && document.getElementById(`ff-back-${secid}`))
                    document.getElementById(`ff-back-${secid}`).click();
                return this.showPopup(title, message, footer, {});
            }
        }
        var curr = this;
        return new Promise((resolve, reject) => {
            try {
                var cartItems = curr.getCartItems() || [];
                var prm = curr.checkInventoryForCartItems(secid, cartItems);
                return prm.then(_ => {
                    var itemsOutOfStock = _.itemsOutOfStock;
                    if (itemsOutOfStock.length > 0 && itemsOutOfStock[0]) {
                       return curr.showWarningForOutOfStockItems(secid, itemsOutOfStock[0]);
                    }
                    return curr.submit(frm, secid, paymentPromise);
                }).catch(error => {
                    console.warn('Error while checking inventory');
                    console.error(error);
                    return curr.submit(frm, secid, paymentPromise);
                });
            }
            catch (error) {
                console.warn('Error while checking inventory');
                console.error(error);
                return curr.submit(frm, secid, paymentPromise);
            }
        });
    }

    this.submit = function(frm, secid, callback)
    {
        var invalids = secid=='-3'?0:this.validate(frm, secid);
        if(invalids > 0) {
            this.getDocument().querySelectorAll('#ff-submit-'+secid+' img').forEach(function(elm){ 
                elm.src = 'https://neartail.com/img/send.svg'; 
            });
            return;
        }
        if(this.submitting) return;
        this.showMessage(secid);
        var curr = this;

        var fc = curr.data.facade || {};
        
        if(fc.formfillable && this.consentAgreed != true) {
            var fc = this.data.facade;
            var submitSec = (fc && fc.submit && fc.submit[secid]) || {};
            if(submitSec.consent) {
                this.consentSecId = secid;
                this.consentDialog(submitSec);
                return false;
            }
        }

        curr.submitting = Promise.resolve(curr.saving).then(function(){
            var pairs = {};
            var formData = new FormData(frm);
            var next, entry;
            var entries = formData.entries();
            while ((next = entries.next()) && next.done === false) 
            {
                entry = next.value;
                var val = pairs[entry[0]];
                if(val)
                    val.push(entry[1]);
                else if(entry[1])
                    pairs[entry[0]] = [entry[1]];
            }
            var forTask = {draftSeq:true, submitSeq:true, paymentId:true, consumerId:true,
                products:true, quantity:true, amount:false, email:false, phone:false};
            for(var tnm in forTask)
            {
                var tval = forTask[tnm];
                if(pairs[tnm])
                    pairs[tnm] = pairs[tnm];
                else if(tval==true)
                    pairs[tnm] = curr.draft[tnm];
                else if(tnm && tval)
                {
                    if(tnm=='phone')
                        pairs[tnm] = tval;
                    else if(tval=='emailAddress')
                        pairs[tnm] = curr.draft.emailAddress;
                    else
                    {
                        var tent = curr.data.scraped.items[tval];
                        pairs[tnm] = tent?curr.draft.entry[tent.entry]:null;
                    }
                }
            }
            pairs.pageHistory = curr.getPageHistory();
            if(curr.draft.responseId)
                pairs.responseId = curr.draft.responseId;
            if(curr.config.plan=='blocked')
                pairs.plan = 'blocked';
            
            if(fc.setting && fc.setting.skipGoogleSubmit)
                pairs.form = 'native'

            curr.stat('submitting');
            
            if(window.gtag) {
                window.gtag('event', 'submit', {
                    event_label: curr.data.request.params.publishId,
                    value: curr.data.request.params.userId
                });
            }
            return curr.sendData(pairs);
        }).then(rs=>{
            return Promise.resolve(callback?callback(rs):null)
            .then(pass=>rs).catch(fail=>rs);
        }).then(rs=>{
            var publishId = curr.data.request.params.publishId;
            curr.stat('goal');
            curr.result = rs;
            if(rs && rs.code==200)
            {
                curr.createCookie('ff-'+publishId, '', -1);
                if(rs.submitSeq)
                {
                    curr.draft.submitSeq = rs.submitSeq;
                    curr.draft.submitted = new Date().getTime();
                }
                var smtxt;
                var submitto = 'default';
                var fc = curr.data.facade;
                if(fc && fc.submit && fc.submit[secid])
                {
                    var itmsubmit = fc.submit[secid];
                    if(itmsubmit.js)
                    {
                        try{
                            eval(itmsubmit.js);
                        }
                        catch(err){
                            console.error(itmsubmit.js+' failed due to '+err);
                        }
                    }
                    if(itmsubmit.submitto)
                        submitto = itmsubmit.submitto;
                    else if(fc.whatsapp && fc.whatsapp.phone)
                        submitto = 'whatsapp';
                    else if(itmsubmit.onsubmit)
                        submitto = itmsubmit.onsubmit;
                    if(submitto=='custom')
                    {
                        if(itmsubmit.messageMark)
                            curr.result.messageMark = itmsubmit.messageMark;
                        else
                            curr.result.messagePlain = itmsubmit.message;
                    }
                    if(submitto=='ifmsg')
                    {
                        var iftmpl = '${computeCondition("'+secid+'")}';
                        curr.result.messageMark = curr.calculateEngine(iftmpl);
                        if(!curr.result.messageMark) curr.result.messageMark = '(No message)';
                    }
                    else if(submitto=='redirect' && itmsubmit.redirect)
                    {
                        var reurl = curr.computeField(itmsubmit.redirect);
                        if(reurl)
                            window.top.location.href = reurl.trim();
                        else
                            console.error(itmsubmit.redirect+' is not a redirection url');
                        return;
                    }
                    else if(submitto=='whatsapp' && itmsubmit.wamsg)
                    {
                        smtxt = curr.computeField(itmsubmit.wamsg);
                    }
                    var {status} = curr.config.mobile||{};
                    var {slug} = curr.config.perma||{};
                    if(status=='active' || status == 'LOGGED_IN')
                    {
                        var mapping = curr.data.facade.mapping||{};
                        var emailfld = curr.data.scraped.items[mapping.email]||{};
                        var emailval = curr.draft.entry[emailfld.entry];
                        if(emailval && curr.result.messageMark)
                        {
                            curr.result.messageMark += `
<div class="ff-cta-center" id="ff-download-mobile-app" style="margin-top:40px;">
    <h4>You can download Neartail app to track status and message us easily.</h4>
    <a href="https://apps.apple.com/app/id6450004218" target="_blank" class="ff-app-install">
        <img src="https://near.tl/images/app-store-badge.svg" style="width:150px;">
    </a>
    <a href="https://play.google.com/store/apps/details?id=com.neartale" target="_blank" class="ff-app-install">
        <img src="https://near.tl/images/play-store-badge.svg" style="padding-left:10px; width:158px;">
    </a>
</div>
                            `;
                        }
                    }
                }
                var phn = curr.getPhone(secid);
                if(phn)
                {
                    curr.draft.waphone = phn;
                    var ph = phn.match(/\d+/g).join('');
                    curr.render();
                    if(!smtxt)
                    {
                        var sfrm = curr.data.scraped||{};
                        var spref = sfrm.title||sfrm.form||'Untitled';
                        spref = '*'+spref+'* #'+curr.draft.submitSeq+'\n\n';
                        smtxt = spref+curr.computeField('${TEXTSUMMARY(true, true, "*")}');
                        var responseTitle = "Summary";
                        if(curr.data && curr.data.facade && curr.data.facade.setting && curr.data.facade.setting.currency) responseTitle = "Invoice";
                        smtxt += `\n\n${responseTitle}: https://near.tl/inbox/${curr.draft.savedId}`;
                        smtxt = smtxt+'\n(Press send to confirm)';
                    }
                    var phurl = 'https://wa.me/'+ph+'?text='+encodeURIComponent(smtxt);
                    if(curr.isMobile())
                    {
                        setTimeout(function(){
                            window.top.location.href = phurl;
                        }, 500);
                    }
                    else
                    {
                        //var wawin = window.open(phurl, '_blank');
                        var qrhtml = `
                            <div class="ff-wa-qrcode form-group ff-item ff-section_header ff-full-width ff-item-noprd">
                                <h4 class="ff-section-header ff-whatsapp-submission-title">Scan this QR code to confirm your order!</h4>
                                <img class="ff-whatsapp-submission-qr" src="https://neartail.com/payment/qrcode/generate?url=${encodeURIComponent(phurl)}"><br>
                                <div class="ff-description">
                                    Once you scan this QR code, you will prompted to submit this order on WhatsApp.
                                    If WhatsApp is already installed on this computer, 
                                    <a href="${phurl}" class="ff-whatsapp-submission-link">click here to continue</a>. 
                                </div>
                            </div>
                        `;
                        var su = document.getElementById('ff-success');
                        if(su) su.innerHTML = qrhtml;
                    }
                    setTimeout(function(){
                        var su = document.getElementById('ff-success');
                        var suhide = document.getElementById('ff-success-hide');
                        if(su && suhide) su.innerHTML = suhide.innerHTML;
                    }, (curr.isMobile()?10:1000)*1000);
                }
                else
                {
                    curr.render();
                }
                curr.scrollIntoView();
                curr.getDocument().querySelectorAll('.ff-payment-form')
                .forEach(elm=>elm.style.display='none');
            }
            else if(rs && rs.code)
            {
                curr.getDocument().querySelectorAll('#ff-submit-'+secid+' img').forEach(function(elm){ 
                    elm.src = 'https://neartail.com/img/send.svg'; 
                });
                throw new Error('Not able to update this response in Google Forms');
                frm.action = 'https://docs.google.com/forms/d/e/'+publishId+'/viewform';
                frm.method = 'GET';
                frm.submit();
            }
            else
            {
                formFacade.render();
            }
            var onsubmit = curr.data.request.query.onsubmit;
            if(window.cartSidebar) cartSidebar.fetch('submit');
            if(window.facadeListener) facadeListener.onChange('submit', curr);
            if(onsubmit && window[onsubmit])
            {
                window[onsubmit](curr);
            }
        }).catch(function(err){
            var msg = err.toString()+'. Submit it again. Contact owner, if this error occurs repeatedly.';
            var footer = `<button class="btn btn-lg btn-primary" id="resubmit">Resubmit</button>`;
            curr.showError('Submit failed', msg, footer);
            var resubmit = function()
            {
                curr.closePopup(false);
                curr.submitWithInventory(frm, secid);
            }
            document.getElementById('resubmit').addEventListener('click', resubmit);
            curr.submitting = null;
        });
        return false;
    }

    this.confirmwa = function(wa)
    {
        var curr = this;
        var baseurl = 'https://formfacade.com';
        if(curr.data.devEnv)
            baseurl = 'http://localhost:5000';
        var params = curr.data.request.params;
        if(curr.draft.savedId)
        {
            var url = baseurl+'/draft/'+params.publishId+'/whatsapp/'+curr.draft.savedId;
            var http = new XMLHttpRequest();
            http.open('POST', url, true);
            http.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
            http.send('phone='+encodeURIComponent(wa));
        }
        delete curr.data.facade.whatsapp.askwa;
        curr.render();
        return false;
    }

    this.savePayment = function()
    {
        var curr = this;
        var baseurl = 'https://formfacade.com';
        if(curr.data.devEnv)
            baseurl = 'http://localhost:5000';
        var params = curr.data.request.params;
        if(curr.draft.savedId)
        {
            var url = baseurl+'/draft/'+params.publishId+'/payment/'+curr.draft.savedId;
            var http = new XMLHttpRequest();
            http.open('POST', url, true);
            http.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
            http.send('status=paid not verified');
        }
    }

    this.submitData = function(nmval)
    {
        var pairs = {id:this.data.request.params.publishId};
        var frm = this.data.scraped;
        var items = frm.items;
        for(var itemId in items)
        {
            var item = items[itemId];
            var val = nmval[item.title];
            if(val && item.entry) 
                pairs['entry.'+item.entry] = val;
        }
        return this.sendData(pairs);
    }

    this.sendData = function(pairs, trgurl)
    {
        var curr = this;
        return new Promise(function(resolve, reject){
            var baseurl = 'https://formfacade.com';
            if(curr.data.devEnv)
                baseurl = 'http://localhost:5000';
            var url = baseurl+(trgurl?trgurl:'/submitForm');
            var params = curr.data.request.params;
            var savedId = curr.draft.savedId;
            if(!savedId) savedId = curr.readCookie('ff-'+params.publishId);
            if(!trgurl && params.userId && params.publishId)
            {
                if(savedId)
                    url = url+'/'+params.userId+'/form/'+params.publishId+'/draft/'+savedId;
                else
                    url = url+'/'+params.userId+'/form/'+params.publishId+'/draft';
            }
            var {originId, originTime} = curr.config||{};
            var params = `callback=callbackFormFacade&originId=${originId}&originTime=${originTime}`;
            for(var nm in pairs)
            {
                var val = pairs[nm];
                if(val && Array.isArray(val))
                {
                    val.forEach(function(ival){
                        params += '&'+nm+'='+encodeURIComponent(ival);
                    });
                }
                else if(val)
                    params += '&'+nm+'='+encodeURIComponent(val);
            }
            var http = new XMLHttpRequest();
            http.open('POST', url, true);
            http.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
            http.onload = function()
            {
                try
                {
                    var jso = JSON.parse(http.response);
                    if(http.status==200)
                        resolve(jso);
                    if(http.status==201)
                        resolve(jso);
                    else if(http.status>=400)
                        reject(jso);
                }
                catch(err)
                {
                    reject(err);
                }
            }
            http.onerror = err=>reject(http.response||"Couldn't connect to server");
            http.send(params);
        });
    }

    this.getHistory = function(eml)
    {
        if(!eml) return;
        var curr = this;
        eml = eml.toLowerCase();
        var {userId, publishId} = this.data.request.params;
        var baseurl = this.data.devEnv?'http://localhost:5000':'https://formfacade.com';
        var fetchurl = `${baseurl}/draft/${userId}/history/${publishId}?email=${encodeURIComponent(eml)}`;
        fetch(fetchurl).then(req=>req.json()).then(history=>{ 
            curr.draft.history = history||null;
            curr.saveDraft();
        });
    }

    this.getPrice = function(item, crncy='$')
    {
        if(item.price) return item.price;
        var oitems = this.data.facade.items?this.data.facade.items:{};
        var oitem = oitems[item.id];
        var prc = {min:0};
        if(oitem && oitem.price)
            prc.min = oitem.price;
        else if(item.help && item.help.indexOf('${')<0 && item.help.indexOf(crncy)>=0)
            prc.min = formFacade.computeField('${price("'+item.help+'","'+crncy+'")}');
        if(prc.min==0)
        {
            if(item.type=='LIST' || item.type=='MULTIPLE_CHOICE' || item.type=='SCALE' || item.type=='CHECKBOX')
            {
                if(item.choices)
                {
                    var chs = item.choices.map(ch=>{
                        if(ch.value.indexOf(crncy)>=0)
                            return formFacade.computeField('${price("'+ch.value+'","'+crncy+'")}');
                        else
                            return 0;
                    });
                    chs.sort((a,b)=>a-b);
                    prc.min = chs[0];
                    prc.max = chs.pop();
                }
            }
            else if(oitem && oitem.measure=='Configurable')
            {
                var vrns = oitem.variants||{};
                var chs = Object.values(vrns).filter(vrn=>!vrn.remain||vrn.remain>0)
                    .map(vrn=>Number(vrn.price));
                chs.sort((a,b)=>a-b);
                prc.min = chs[0];
                prc.max = chs.pop();
            }
            else if(item.type=='GRID')
            {
                if(item.rows)
                {
                    var chs = item.rows.map(ch=>{
                        if(ch.value.indexOf(crncy)>=0)
                            return formFacade.computeField('${price("'+ch.value+'","'+crncy+'")}');
                        else
                            return 0;
                    });
                    chs.sort((a,b)=>a-b);
                    prc.min = chs[0];
                    prc.max = chs.pop();
                }
            }
        }
        if(prc.min>0) prc.minformat = formFacade.computeField('${format('+prc.min+',"'+crncy+'")}');
        if(prc.max>0) prc.maxformat = formFacade.computeField('${format('+prc.max+',"'+crncy+'")}');
        if(oitem && oitem.fullprice>0) prc.fullformat = formFacade.computeField('${format('+oitem.fullprice+',"'+crncy+'")}');
        item.price = prc;
        return prc;
    }

    this.formatWeight = function(val)
    {
        if(isNaN(val)) return val;
        var nval = new Number(val);
        var unit = 'kg';
        var fac = this.data.facade;
        if(fac.setting&&fac.setting.currencyCode=='USD')
            unit = 'lb';
        if(unit=='kg')
        {
            if(nval<1)
                return (nval*1000)+' gm';
            else
                return nval+' kg';
        }
        else
        {
            if(nval<1)
                return (nval*16)+' oz';
            else
                return nval+' lb';
        }
    }

    this.hasProducts = function(sec)
    {
        var oitems = this.data.facade.items?this.data.facade.items:{};
        var prds = sec.items.filter(function(item, itmi){
            var oitem = oitems[item.id]?oitems[item.id]:{};
            if(oitem.widget=='product') return item;
        });
        return prds.length;
    }

    this.getSections = function(flush)
    {
        if(this.__sections && !flush) return this.__sections;
        var frm = this.data.scraped;
        var itmlen = 0;
        if(frm && frm.items)
            itmlen = Object.keys(frm.items).length;
        if(itmlen > 0)
        {
            var curr = this;
            var fac = this.data.facade;
            if(!fac) fac = {};
            if(!fac.setting) fac.setting = {};
            var oitems = fac.items?fac.items:{};
            var unit = fac.setting.currencyCode=='USD'?'lb':'kg';
            var officeUseSections = fac.setting.officeUseSections||{};
            var officeuseSectionIds = Object.keys(officeUseSections);
            if(false && this.isEditMode() && this.hasCreatorOrEditor())
            {
                for(var iid in frm.items)
                {
                    var sitm = frm.items[iid]||{};
                    var fitm = oitems[iid]||{};
                    sitm.index = fitm.i||sitm.index;
                }
            }
            this.__sections = this.asSections(frm);
            this.__sections.forEach((sec, s)=>{
                sec.items.filter(itm=>{
                    var excludes = ['SECTION_HEADER','PAGE_BREAK','DATE','TIME','IMAGE','VIDEO'];
                    return excludes.indexOf(itm.type)<0;
                }).forEach(function(item, itmi){
                    var oitem;
                    if(oitems[item.id]) {
                        oitem = oitems[item.id];
                    } else {
                        oitems[item.id] = {};
                        oitem = oitems[item.id]
                    }
                    if(oitem.deleted) item.deleted = oitem.deleted;
                    if(curr.data.fulledit)
                    {
                        if(oitem.mode=='hide' || oitem.mode=='officeuse')
                            oitem.mode = 'edit';
                    }
                    else if(curr.data.restoreId)
                    {
                        if(curr.data.appearance=='officeuse')
                        {
                            if(oitem.mode=='officeuse')
                                oitem.mode = 'edit';
                            else if(oitem.mode=='hide')
                                oitem.mode = 'hide';
                            else
                                oitem.mode = 'read';
                        }
                        else
                        {
                            if(oitem.mode=='officeuse')
                                oitem.mode = 'hide';
                        }
                    }
                    item.price = fac.setting.currency?curr.getPrice(item, fac.setting.currency):{};
                    item.product = (item.price.min>0||item.price.max>0)&&item.titleImage?{i:itmi}:null;
                    if(oitem.widget=='product' || oitem.prdimage) item.product = {i:itmi, noun:fac.setting.noun};
                    if(item.product)
                    {
                        if((item.type=='TEXT' && oitem.choices) || item.basetype=='TEXT')
                        {
                          item.type = 'LIST';
                          item.basetype = 'TEXT';
                          var discnum = oitem.discounted?oitem.discounted.filter(d=>d).length:0;
                          if(discnum==0) delete oitem.discounted;
                          if(oitem.choices)
                          item.choices = oitem.choices.map((och,c)=>{
                            var ch = {value:och};
                            if(oitem.measure=='Weight')
                              ch.display = curr.formatWeight(ch.value);
                            if(oitem.discounted)
                            {
                              ch.discounted = oitem.discounted[c];
                              if(ch.discounted)
                              {
                                var dsp = ch.display?ch.display:ch.value;
                                ch.display = dsp+' <small>'+curr.computeField('${format('+ch.discounted+',"'+fac.setting.currency+'")}')+'</small>';
                              }
                            }
                            return ch;
                          });
                          else {
                            // Enabling spinner widget.
                            item.choices = null;
                            item.type = 'TEXT';
                          }
                          if(oitem.inventory=='yes' && oitem.measure && isNaN(oitem.remain)==false && item.choices)
                          {
                            item.choices = item.choices.filter(ch=>{
                                if(isNaN(ch.value)==false)
                                {
                                    var chval = parseFloat(ch.value);
                                    if(chval>oitem.remain) return false;
                                }
                                return true;
                            });
                          }
                        }
                        else if(item.type=='PARAGRAPH_TEXT' && oitem.variants)
                        {
                            for(var vid in oitem.variants)
                            {
                                var vrn = oitem.variants[vid];
                                if(vrn.price) vrn.display = vrn.name+' <small>'+curr.computeField('${format('+vrn.price+',"'+fac.setting.currency+'")}')+'</small>';
                            }
                            if(oitem.inventory=='yes')
                            {
                                var outs = Object.values(oitem.variants).filter(vrn=>vrn.remain<=0);
                                if(Object.keys(oitem.variants).length==Object.keys(outs).length)
                                    oitem.remain = 0;
                                else 
                                    oitem.remain = 1;
                            }
                        }
                        if(oitem.measure)
                        {
                          // If there are choices, then it is a list of choices, so a placeholder is Select widget type else It is a text input.
                          if(oitem.choices && oitem.choices.length>0) {
                            oitem.placeholder = curr.lang('Select '+oitem.measure);
                          } else {
                            oitem.placeholder = curr.lang('Enter '+oitem.measure);
                          }
                          
                          if(oitem.measure=='Weight') oitem.placeholder += ' ('+unit+')';
                        }
                    }
                });

                if(officeuseSectionIds.length > 0){
                    sec.headers.forEach((header, a) => {
                        if(header.head) {
                            var headerId = header.head.id;
                            var oHeadItem;
                            if(oitems[headerId]) {
                                oHeadItem = oitems[headerId];
                            } else {
                                oitems[headerId] = {};
                                oHeadItem = oitems[headerId]
                            }
                            if(curr.data.fulledit) {
                                if(oHeadItem.mode=='hide' || oHeadItem.mode=='officeuse')
                                    oHeadItem.mode = 'edit';
                            } else if(curr.data.appearance=='officeuse'){ // public mode
                                if(curr.data.officeuseSectionId == headerId) // current officeuse section
                                    oHeadItem.mode = 'edit';
                                else if(curr.officeUseSectionHasEntry(header)) // already filled officeuse section
                                    oHeadItem.mode = 'read';
                                else if(officeuseSectionIds.indexOf(headerId) > -1){ // not yet filled officeuse section
                                    oHeadItem.mode = 'read';
                                    // If office use section not filled by approver, hide fields & only show the header
                                    if(curr.data.facade.titles && curr.data.facade.titles[headerId]){
                                        var ttl = curr.data.facade.titles[headerId];
                                        ttl.messageMark = '-- Not yet completed --'
                                    } else {
                                        if(!curr.data.facade.titles) curr.data.facade.titles = {}
                                        curr.data.facade.titles[headerId] = {messageMark: '-- Not yet completed --'}
                                    }    
                                }

                                // else if(officeuseSectionIds.indexOf(headerId)>-1)
                                //     oHeadItem.mode = 'hide';
                            } else if(officeuseSectionIds.indexOf(headerId)>-1) { // editor & public mode
                                oHeadItem.mode = 'hide';
                            }

                            header.items.forEach((item, i) => {
                                var oitem;
                                if(oitems[item.id]) {
                                    oitem = oitems[item.id];
                                } else {
                                    oitems[item.id] = {};
                                    oitem = oitems[item.id]
                                }
                                if(curr.data.appearance=='officeuse'){ // public mode
                                    // if section is officeuse, show & hide items based on section
                                    if(officeuseSectionIds.indexOf(headerId)>-1) {
                                        oitem.mode = oHeadItem.mode;
                                        // If office use section not filled by approver, hide fields & only show the header
                                        if(curr.data.officeuseSectionId != headerId && !curr.officeUseSectionHasEntry(header))
                                            oitem.mode = 'hide';

                                        if(oitems[item.id] && oitems[item.id].required == 'approver' && oitem.mode == 'edit') {
                                            item.required = 1
                                        }
                                    }
                                } else { // editor & public mode
                                    if(oHeadItem.mode == 'hide')
                                        oitem.mode = oHeadItem.mode;
                                }
                                if(curr.isEditMode()) {
                                    if(oitems[item.id] && oitems[item.id].required == 'approver') {
                                        item.required = 1
                                    }
                                }
                                oitem.isOfficeuseHeader = officeuseSectionIds.indexOf(headerId)>-1
                            });
                        }
                    });
                }

                sec.allItems = sec.items;
                sec.items = sec.items.filter(itm=>!itm.deleted);
            });
            return this.__sections;
        }
        return [];
    }

    this.officeUseSectionHasEntry = function(header) {
        var hasEntries = false;
        var entries = this.draft.entry||{};
        header.items.forEach((item, i) => {
            if(item.entry && entries[item.entry])
                hasEntries = true
        });
        return hasEntries;
    }

    this.splitHeaders = function(headers)
    {
        var curr = this;
        var splits = [];
        var fac = this.data.facade||{};
        var oitems = fac.items||{};
        headers.forEach(header=>{
            var inItems = [];
            var outItems = [];
            header.items.forEach(function(itm, itmi)
            {
                var oitem = oitems[itm.id]||{};
                if(oitem && oitem.measure === 'Nested' && oitem.nested !== 'Appointment')
                {
                    var variants = Object.keys(oitem.variants || {});
                    var variants1 = Object.keys(oitem.variants1 || {});
                    var choices = oitem.choices || [];
                    if(choices.length > 0)
                    {
                        if(Number(choices[choices.length - 1]) <= 0)
                        {
                            return outItems.push(itm);
                        }
                    } else
                    {
                        return outItems.push(itm);
                    }
                    for(let i = 0; i < variants.length; i++)
                    {
                        var variant = variants[i];
                        for(let j = 0; j < variants1.length; j++)
                        {
                            var variant1 = variants1[j];
                            var variantConfig = oitem.variantConfig || {};
                            var inventory = variantConfig.inventory || {};
                            var inven = inventory[variant + "-" + variant1] || {};
                            var remain = inven.remain;
                            if (remain === undefined) remain = "";
                            if (remain > 0 || remain === '') 
                            {
                                return inItems.push(itm);
                            }
                        }
                    }
                    outItems.push(itm);
                }
                else if(oitem.inventory=='yes' && oitem.remain<=0 && oitem.mode !== 'hide')
                    outItems.push(itm);
                else
                    inItems.push(itm);
            });
            var inHeader = Object.assign({}, header, {items:inItems});
            splits.push(inHeader);
            if(outItems.length>0)
            {
                var outItem = outItems[0]||{};
                var outHead = {
                    outstock:'out-'+outItem.id, title:curr.lang('Out of stock'),
                    help:curr.lang('Order early to avoid missing out next time')
                };
                var outHeader = {outstock:true, head:outHead, items:outItems};
                splits.push(outHeader);
            }
        });
        return splits;
    }

    this.validate = function(frm, secid)
    {
        var curr = this;
        var invalids = [];
        var doc = this.getDocument();
        var frmdata = new FormData(frm);
        var sections = this.getSections(true);
        var section = sections[0];
        sections.forEach(function(sec, s){
            if(sec.id==secid)
                section = sec;
        });
        doc.querySelectorAll('#ff-sec-'+section.id+' .ff-widget-error').forEach(function(widerr){
            widerr.style.display = 'none';
        });
        var emlwid = doc.getElementById('WidgetemailAddress');
        if(emlwid && emlwid.checkValidity()==false)
        {
            var widerr = doc.getElementById('ErroremailAddress');
            if(emlwid.value)
                widerr.innerHTML = '<b>!</b>'+curr.lang('Must be a valid email address');
            else
                widerr.innerHTML = '<b>!</b>'+curr.lang('Please fill this');
            widerr.style.display = 'block';
            invalids.push(emlwid);
        }
        section.items.forEach(function(itm, i){
            var widinp = doc.querySelector('#ff-id-'+itm.id+' input');
            if(itm.type=='PARAGRAPH_TEXT')
                widinp = doc.querySelector('#ff-id-'+itm.id+' textarea');
            else if(itm.type=='LIST')
            {
                widinp = doc.querySelector('#ff-id-'+itm.id+' select');
                if(!widinp)
                    widinp = doc.querySelector('#ff-id-'+itm.id+' input');
            }
            var reportError = function(msg)
            {
                invalids.push(widinp);
                var widerr = doc.getElementById('Error'+itm.id);
                if(widerr)
                {
                    widerr.innerHTML = '<b>!</b>'+msg;
                    widerr.style.display = 'block';
                }
            }
            var envalue;
            var valid = true;
            if(widinp)
            {
                if(widinp.readOnly)
                {
                    widinp.readOnly = false;
                    valid = widinp.checkValidity();
                    widinp.readOnly = true;
                }
                else
                    valid = widinp.checkValidity();
                envalue = frmdata.get(widinp.name);
            }
            if(valid==false)
            {
                if(itm.required && !envalue)
                {
                    reportError(curr.lang('Please fill this'));
                }
                else if(envalue)
                {
                    if(widinp.type=='email')
                        reportError(curr.lang('Must be a valid email address'));
                    else if(widinp.type=='date') 
                    {
                        if(itm && itm.logic && itm.logic.validation && (itm.logic.validation.validOperator === 'FutureLead' && itm.logic.validation.validValue))
                        {
                            reportError(curr.lang(`Invalid date selection. Please choose a date that is at least ${itm.logic.validation.validValue} days from today.`));
                        }
                        else if (itm && itm.logic && itm.logic.validation && (itm.logic.validation.validOperator === 'Between')) 
                        {
                            reportError(curr.lang(`Invalid date selection. Please choose a date between ${itm.logic.validation.validValue} and ${itm.logic.validation.validValue2}.`));
                        }
                        else if(itm && itm.logic && itm.logic.validation && (itm.logic.validation.validOperator === 'Future' || itm.logic.validation.validFuture == 1))
                        {
                            reportError(curr.lang('Invalid date selection. Please choose a future date.'));
                        }
                        else
                        {
                            reportError(curr.lang('Invalid date'));
                        }
                    }
                    else if(widinp.type=='datetime-local')
                        reportError(curr.lang('Invalid date'));
                    else
                        reportError(curr.lang('Invalid input'));
                }
                else
                {
                    reportError(curr.lang('Invalid input'));
                }
            }
            else if(widinp && widinp.list && envalue && itm.choices)
            {
                var matches = itm.choices.filter(ch=>ch.value==envalue.trim());
                if(matches.length==0) reportError(curr.lang('Invalid answer. Clear & select a valid answer from the list'));
            }
            else
            {
                if(curr.data.facade && curr.data.facade.items)
                    itm.overwrite = curr.data.facade.items[itm.id];
                curr.validateEngine(itm, frmdata, reportError);
            }
        });
        if(invalids.length>0)
        {
            invalids[0].focus();
            this.scrollIntoView(invalids[0]);
        }
        return invalids.length;
    }

    this.getPairs = function(frm)
    {
        var pairs = {};
        var next, entry;
        var formData = frm?new FormData(frm):new FormData();
        var entries = formData.entries();
        while ((next = entries.next()) && next.done === false) 
        {
            entry = next.value;
            var val = pairs[entry[0]];
            if(val)
                val.push(entry[1]);
            else if(entry[1])
                pairs[entry[0]] = [entry[1]];
        }
        return pairs;
    }

    this.getNextSectionId = function(secid)
    {
        var sections = this.getSections();
        var fac = this.data.facade||{};
        var fcitms = fac.items||{};
        var valids = sections.filter(sec=>{
            var prds = sec.items.filter(itm=>itm.product);
            if(prds.length>0)
            {
                var stocked = prds.filter(prd=>{
                    var fitm = fcitms[prd.id]||{};
                    return fitm.mode=='hide'||(fitm.inventory=='yes'&&fitm.remain<=0)?false:true;
                });
                return stocked.length>0;
            }
            return true;
        });
        var idx = valids.findIndex(sec=>sec.id==secid);
        var nxt = valids[idx+1];
        if(nxt) return nxt.id;
        idx = sections.findIndex(sec=>sec.id==secid);
        nxt = sections[idx+1];
        if(nxt) return nxt.id;
        return secid;
    }

    this.gotoSection = function(frm={}, secid, deftrg)
    {
        var doc = this.getDocument();
        var trg;
        if(deftrg == 'back')
        {
            trg = this.draft.pageHistory.pop();
            var fac = this.data.facade||{};
            if(fac.neartail || fac.whatsapp)
            {
                this.draft.pageHistory.unshift(trg);
                this.draft.pageHistory.unshift(secid);
            }
        }
        else
        {
            this.saveDraft();
            var invalids = this.validate(frm, secid);
            if(invalids > 0) return;
            trg = deftrg?deftrg:this.getNextSectionId(secid);
            var items = this.data.scraped.items||{};
            doc.querySelectorAll('#ff-sec-'+secid+' .ff-nav-dyn').forEach(function(wid={},w){
                var navs = [];
                var fid = wid.id?wid.id.split('-').pop():null;
                var itm = items[fid]||{};
                var enval = frm['entry.'+itm.entry]||{};
                if(itm.choices) navs = itm.choices.filter(ch=>ch.value==enval.value);
                if(navs.length>0) trg = navs[0].navigateTo;
            });
            if(trg == -1)
                trg = secid;
            else if(trg == -2)
                trg = this.getNextSectionId(secid);
            else if(trg == -3)
                trg = 'ending';
        }
        this.jumptoSection(frm, secid, deftrg, trg);
    }

    this.directtoSection =function(trg, wid)
    {
        document.body.style.overflowY = 'auto';
        var frm = this.getContentElement().querySelector('form');
        var secid = this.draft.activePage||'root';
        this.jumptoSection(frm, secid, secid, trg, wid);
    }

    this.login = function(loggedin={})
    {
        var curr = this;
        if(!loggedin.sub) return;
        this.draft.consumerId = loggedin.sub;
        var {mapping} = this.data.facade||{};
        if(!mapping) mapping = {};
        var {items} = this.data.scraped||{};
        if(!items) items = {};
        var entries = this.draft.entry||{};
        var publishId = this.data.request.params.publishId;
        var prefix = '';
        if(window && window.location && window.location.hostname === 'near.tl') {
            prefix = 'https://neartail.com';
        }
        return fetch(prefix + '/consumer/'+publishId+'/google/'+loggedin.sub, {
            method:'post', body:JSON.stringify(loggedin),
            headers:{accept:'application/json', 'content-type':'application/json'}
        }).then(response=>response.json()).then(function(consumer){
            for(var nm in consumer)
            {
                var val = consumer[nm];
                var iid = mapping[nm];
                var item = items[iid]||{};
                if(item.entry && val)
                {
                    var exval = entries[item.entry];
                    if(!exval) entries[item.entry] = val;
                }
            }
            curr.draft.entry = entries;
            curr.render();
            return curr.saveDraft();
        }).catch(err=>err);
    }
    this.zoomImage = (ele) => {
        var activeIndex = ele.getAttribute('activeIndex');
        if(activeIndex == undefined) {
            activeIndex = "-1";
        }
        document.querySelector("[prd-img-index='"+activeIndex+"']").click();
    }
    this.scrollAdditionalImage = (type) => {
        if(type === 'down') {
            // scroll to end  document.getElementById('ff-prdadditionalimgcontainer-wrapper').
            document.querySelector('.ff-prdimglast').scrollIntoView({behavior: "smooth", block: "end", inline: "nearest"});

            // disable down arrow and enable up arrow.
            document.querySelector('.ff-downarrow-button').style.display = 'none';
            document.querySelector('.ff-uparrow-button').style.display = 'flex';
        }else {
            // scroll to top
            document.querySelector('.ff-prdimgfirst').scrollIntoView({behavior: "smooth", block: "start", inline: "nearest"});

            document.querySelector('.ff-downarrow-button').style.display = 'flex';
            document.querySelector('.ff-uparrow-button').style.display = 'none';
        }
    }
    this.changeAdditionalImage = (ele) => {
        document.querySelector('.ff-prdimg.ff-prdimage-zoom').src = ele.src;
        document.querySelector('.ff-prdimg.ff-prdimage-zoom').setAttribute('activeIndex', ele.getAttribute('index'));
        document.querySelectorAll('.ff-prdimg-thumbnail.ff-prdimg-active').forEach((ele) => {
            ele.classList.remove('ff-prdimg-active');
        });
        ele.classList.add('ff-prdimg-active');
    }

    this.checkOneTapLogin = function(){
        var curr = this;
        var {setting} = this.data.facade||{};
        var {loginpage} = setting||{};
        var activePage = this.draft.activePage;
        if(loginpage==activePage && window.loadOneTap && !this.draft.consumerId) {
            let isEditMode = this.data && (this.data.fulledit || this.data.officeuseSectionId);
            if(!isEditMode)
                loadOneTap().then(loggedin=>curr.login(loggedin)).catch(err=>err);
        }
    }

    this.jumptoSection = function(frm, secid, deftrg, trg, wid)
    {
        var curr = this;
        this.scrapeSection(this.getPageHistory());
        this.draft.activePage = trg;
        this.checkOneTapLogin();
        this.render();
        if(wid)
        {
            var elm = document.getElementById('ff-id-'+wid)||{};
            if(elm.scrollIntoView)
            {
                setTimeout(() => {elm.scrollIntoView(true);}, 100);
            }
        }
        else
        {
            this.scrollIntoView();
        }
        if(deftrg=='back') return;
        this.draft.pageHistory.push(secid);
        if(window.gtag) window.gtag('event', 'goto', {
            event_category:'formfacade',
            event_label:this.data.request.params.publishId+'-'+secid,
            value:this.draft.pageHistory.length
        });
    }

    this.updateProgressBar = (trg, deftrg) => {
        if(!document || !document.querySelector('#ff-page-progress') || !document.querySelector("#ff-page-progress-start-label"))
        {
            return;
        }

        var pages = formFacade.getSections();
        for(let i = 0; i < pages.length; i++) {
            if(pages[i].id == trg) {
                document.querySelector('#ff-page-progress').value = i + 1;
                document.querySelector("#ff-page-progress-start-label").innerHTML = i + 1;
                break;
            }
        }
    }

    this.scrollIntoView = function(elm)
    {
        if(!elm) elm = this.getContentElement()||{};
        // If the elm is hidden, then scroll to the parent. (To fix the following issue, uppy file uploads hide the normal input component and create a new component for file upload.)
        if(elm.type=='hidden')
        {
            elm = elm.parentElement;
        }

        if(elm.scrollIntoView)
        {
            if (elm.id == 'ff-compose') {
                elm.scrollIntoView({ behavior: "smooth", block: "start", inline: "nearest" });
            }
            const viewportHeight = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0);

            // Calculate the offset from the entire page
            let offsetTop = elm.offsetTop;
            let offsetParent = elm.offsetParent;
            while (offsetParent) {
                offsetTop += offsetParent.offsetTop;
                offsetParent = offsetParent.offsetParent;
            }

            // Calculate the scroll position to place the element either in the center or 20-30% from the top
            const scrollPosition = offsetTop - (viewportHeight * 0.3);

            if (scrollPosition < 0) return;

            // Scroll to the calculated position
            window.scrollTo({
                top: scrollPosition,
                behavior: 'smooth'
            });
            try {
                if(window.parent && window.parent.scrollIntoView) {
                    window.parent.scrollIntoView();
                }
            }catch(error) {
                // console.log('Error in scrolling into view', error);
            }
            
        }
    }

    this.scrapeSection = function(pghistory)
    {
        var curr = this;
        //if(!curr.data.devEnv) return false;
        var elm = this.getContentElement();
        if(!elm) return Promise.resolve();
        var frm = elm.querySelector('form');
        var pairs = curr.getPairs(frm);
        var publishId = this.data.request.params.publishId;
        if(pghistory)
        {
            pairs.pageHistory = pghistory;
            pairs.continue = 1;
        }
        return this.sendData(pairs, '/nextSection/'+publishId).then(function(rs={}){
            if(!rs.images) return;
            if(!curr.data.form) curr.data.form = {};
            var imgs = curr.data.form.images;
            curr.data.form.images = Object.assign(imgs||{}, rs.images);
        }).catch(function(err){
            console.warn('nextSection failed with '+err);
        });
    }

    this.getPageHistory = function()
    {
        var curr = this;
        var secarr = [];
        var doc = this.getDocument();
        var secs = doc.querySelectorAll('.ff-section');
        secs.forEach(function(sec, s){
            var secid = sec.id.split('-').pop();
            var secjso = curr.data.scraped.items[secid];
            if(curr.draft.pageHistory.indexOf(secid)>=0 || curr.draft.activePage==secid)
                 secarr.push(secid=='ending'?'-3':s);
        });
        return secarr.join(',');
    }

    this.getDocument = function()
    {
        return document;
    }

    this.lang = function(txt, opt)
    {
        if(this.langtext && this.langtext[txt])
        {
            txt = this.langtext[txt];
        }
        if(txt && opt)
        {
            for(var nm in opt)
            {
                var vl = opt[nm];
                txt = txt.split('$'+nm).join(vl);
            }
        }
        return txt;
    }

    this.isMobile = function()
    {
        var check = false;
        (function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino|android|ipad|playbook|silk/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4))) check = true;})(navigator.userAgent||navigator.vendor||window.opera);
        return check;
    }

    this.isIos = function()
    {
        return (/iphone|ipad|ipod/gi.test(navigator.userAgent)) || (navigator.userAgent.match(/iPad|Macintosh/i) !== null && 'ontouchend' in document);
    }

    this.isTestUser = function(userId)
    {
		return ['115950622256526421100','114565391771428289104','105242287939464144485', '111716875395075072856', '110010512446331946668'].indexOf(userId) > -1
	}

    this.uploadFile = function(fld, entry, widg)
    {
        var curr = this;
        var doc = curr.getDocument();
        var stf = doc.getElementById('Status'+fld);
        if(stf) stf.innerHTML = 'Uploading...';
        return new Promise(function(resolve, reject){
            var publishId = curr.data.request.params.publishId;
            var savedId = curr.draft&&curr.draft.savedId?curr.draft.savedId:'none';
            var url = 'https://formfacade.com/upload/'+publishId+'/'+savedId+'/'+entry;
            var formData = new FormData();
            formData.append('file', widg.files[0]);
            var http = new XMLHttpRequest();
            http.open('POST', url, true);
            http.onload = function()
            {
                try
                {
                    var jso = JSON.parse(http.response);
                    if(http.status==200)
                        resolve(jso);
                    if(http.status==201)
                        resolve(jso);
                    else if(http.status>=400)
                        reject(jso);
                }
                catch(err)
                {
                    reject(err);
                }
            }
            http.onerror = err=>reject(http.response||"Couldn't connect to server");
            http.send(formData);
        }).then(function(jso){
            curr.draft.savedId = jso.savedId;
            var hdn = doc.getElementById('Widget'+fld);
            if(hdn)
            {
                hdn.value = jso.file;
                hdn.dispatchEvent(new Event('change', {bubbles:true}));
            }
            if(stf) stf.innerHTML = jso.file.split('/').pop();
        });
    }

    this.getPaymentButtons = function()
    {
        var paybtns = [];
        if(!this.data.scraped.items)
            return paybtns;
        var fac = this.data.facade||{};
        var enh = fac.enhance||{};
        if(enh.closed=='on')
            return paybtns;
        var sbmt = fac.submit||{};
        for(var secid in sbmt)
        {
            var secbtn = sbmt[secid]||{};
            if(secbtn.payConfig=='peergateway')
            {
                secbtn.id = secid;
                paybtns.push(secbtn);
            }
            else if(secbtn.payConfig=='disabled')
            {
            }
            else if(secbtn.amountFrom)
            {
                secbtn.id = secid;
                paybtns.push(secbtn);
            }
        }
        return paybtns;
    }
    this.showPaymentWithCheckInventory = function(frm, secid)
    {
        var curr = this;
        return new Promise((resolve, reject) => {
            try {
                var cartItems = curr.getCartItems() || [];
                var prm = curr.checkInventoryForCartItems(secid, cartItems);
                return prm.then(_ => {
                    var itemsOutOfStock = _.itemsOutOfStock;
                    if (itemsOutOfStock.length > 0 && itemsOutOfStock[0]) {
                       return curr.showWarningForOutOfStockItems(secid, itemsOutOfStock[0]);
                    }
                    return curr.showPayment(frm, secid);
                }).catch(error => {
                    console.warn("Error in checking inventory");
                    console.error(error);
                    return curr.showPayment(frm, secid);
                });
            }
            catch (error) {
                console.warn("Error in checking inventory");
                console.error(error);
                return curr.showPayment(frm, secid);
            }
        });
    }

    this.showPayment = function(frm, secid)
    {
        var invalids = this.validate(frm, secid);
        if(invalids > 0) {
            this.getDocument().querySelectorAll('#ff-submit-'+secid+' img').forEach(function(elm){ 
                elm.src = 'https://neartail.com/img/send.svg'; 
            });
            return;
        }
        // scroll to ff-form class on validation success.
        const formElement = document && document.querySelector('.ff-form');
        if(formElement)
        {
            formElement.scrollIntoView();
        }
        this.draft.pageHistory.push(secid);
        var doc = this.getDocument();
        var elms = doc.querySelectorAll('#ff-submit-'+secid+' img');
        elms.forEach(elm=>elm.src = 'https://formfacade.com/img/loading.svg');
        var fac = this.data.facade?this.data.facade:{};
        var itms = this.data.scraped.items||{};
        var mapping = fac.mapping||{};
        var sbmt = fac.submit?fac.submit:{};
        var btn = sbmt[secid]||{};
        var itm;
        if(btn.amountFrom)
            itm = itms[btn.amountFrom];
        else if(mapping['net-amount'])
            itm = itms[mapping['net-amount']];
        else if(mapping.amount)
            itm = itms[mapping.amount];
        var amt;
        if(itm && itm.entry){
            var amt = this.draft.entry[itm.entry];
            if(amt) this.draft.amount = amt;
            if(isNaN(amt)) {
                this.showPopup('Submit failed', '<span style="color:red">âš  Amount is not configured correctly. Please contact your admin to resolve.</span>');
                this.showWarning('Invalid amount configuration', 'You have mapped a text field for Amount. Please select the correct field for the Amount option.', null, {ignorePopup: true});
                elms.forEach(elm=>elm.src = 'https://formfacade.com/img/send.svg');
                return false;
            }
        } else {
            this.showPopup('Submit failed', '<span style="color:red">âš  Amount is not configured correctly. Please contact your admin to resolve.</span>');
            var errText = mapping['net-amount'] ? 'Net Amount': 'Amount';
            this.showWarning('Invalid amount configuration', `You have not mapped the amount field required for payment. Please select the correct field for the ${errText} option`, null, {ignorePopup: true});
            elms.forEach(elm=>elm.src = 'https://formfacade.com/img/send.svg');
            return false;
        }

        if(btn.payConfig=='peergateway' && btn.configId)
            this.showPeerPayment(secid, amt);
        else
            this.showCardPayment(secid, amt);

        if(formElement)
        {
            formElement.scrollIntoView();
        }
    }

    this.showPeerPayment = function(secid, amt)
    {
        var curr = this;
        this.draft.paymentStatus = 'initiated';
        this.saveDraft();
        this.draft.activePage = secid+'-pay';
        this.render();
        var doc = this.getDocument();
        var elm = doc.getElementById('ff-payment-form-'+secid);
        elm.innerHTML = '<h3>Loading...</h3>';
        const formElement = document.querySelector('.ff-form');
        if(formElement) {
            formElement.scrollIntoView();
        }
        var userId = this.data.request.params.userId;
        var peerhost = this.data.devEnv?'//localhost:3000': '//pay.peergateway.com';
        var scripts = [peerhost+'/js/pay_v1/payment-forms.js?_=v20'];
        Promise.resolve(window.PaymentForm||this.loadScripts(scripts)).then(_=>{
            var keyfields = {};
            ['name', 'email', 'phone', 'address'].forEach(attr=>{
                var iid = curr.data.facade.mapping[attr];
                var itm = iid?curr.data.scraped.items[iid]:null;
                keyfields[attr] = itm?curr.draft.entry[itm.entry]:null;
            });
            var {name, email, phone, address} = keyfields;
            var orderId = curr.draft.savedId;
            var prm = Promise.resolve(orderId);
            if(!orderId) {
                if(curr.saving)
                    prm = curr.saving.then(_=>curr.draft.savedId);   
                else
                    prm = curr.saveDraft().then(_=>curr.draft.savedId);
            }
            prm.then(function(svid){
                orderId = svid;
                var trackingNumber = curr.draft.savedId+'*'+curr.draft.draftSeq;
                var billingDetails = {name, email, phone, orderId, address, trackingNumber};
                var stg = curr.data.facade.setting||{};
                var {currency, currencyCode} = stg;
                var sbmts = curr.data.facade.submit||{};
                var sbmt = sbmts[secid]||{};
                var lineItems = (curr.calculateEngine("${getBill()}", {returntype:true})||[])[0]
                lineItems = curr.calculateEngine("${getBill()}", {returntype:true}).map(function(item){
                    return {name: item[0], quantity: item[2], quantity: item[2], unit_amount: {value: item[1], currency_code: currencyCode}}
                });
                var params = {
                    app:'neartail', userId:userId, configId:sbmt.configId,
                    currency:currency, currencyCode:currencyCode, publishId: curr.data.request.params.publishId,
                    lineItems: lineItems
                };
                window.paymentForm = new PaymentForm(params, {
                    includeEJS: false,
                    translate: curr.lang.bind(curr),
                    walletSelectCallback: function(rs) {
                        var doc = curr.getDocument();
                        var publishId = curr.data.request.params.publishId;
                        var paydtelm = doc.getElementById('PaymentData'+publishId);
                        if(paydtelm) paydtelm.value = JSON.stringify(rs);
                        var rfrm = doc.getElementById('Publish'+publishId);
                        var note;
                        curr.draft.paymentStatus = (rs&&rs.status)||null;
                        var callback = function(rs){
                            curr.createCookie('ff-'+publishId, '', -1);
                            if(rs.submitSeq)
                            {
                                curr.draft.submitSeq = rs.submitSeq;
                                curr.draft.submitted = new Date().getTime();
                            }
                            if(window.cartSidebar) cartSidebar.fetch('submit');
                            if(window.facadeListener) facadeListener.onChange('submit', curr);
                            if(rs.submitSeq) note = curr.paddingWithZero(rs.submitSeq);
                            window.paymentForm.loadPeerGatewayWallets({note: note, orderId: curr.draft.savedId});
                            return new Promise(function(resolve, reject){
                                window.paymentConfirmPromise = resolve;
                            });
                        }
                        curr.submitWithInventory(rfrm, secid, callback);
                    },
                    markAsPaidCallback: function(rs){
                        curr.draft.paymentStatus = 'paid not verified';
                        curr.savePayment();
                        window.paymentConfirmPromise && window.paymentConfirmPromise();
                    },
                    paymentCallback: function(rs){
                        var doc = curr.getDocument();
                        var publishId = curr.data.request.params.publishId;
                        var paydtelm = doc.getElementById('PaymentData'+publishId);
                        if(paydtelm) paydtelm.value = JSON.stringify(rs);
                        var payelm = doc.getElementById('Payment'+publishId);
                        if(payelm && rs.tid) payelm.value = rs.tid;
                        var rfrm = doc.getElementById('Publish'+publishId);
                        curr.draft.paymentStatus = rs.status||null;
                        // curr.submit(rfrm, secid);
                        curr.submitWithInventory(rfrm, secid);
                    }
                });
                window.paymentForm.init('#ff-payment-form-'+secid, amt, billingDetails);
            });
        });
    }

    this.showCardPayment = function(secid, amt)
    {
        var curr = this;
        var baseurl = 'https://neartail.com';
        if(this.data.devEnv)
            baseurl = 'http://localhost:5000';
        var userId = this.data.request.params.userId;
        fetch(
            baseurl+"/payment/"+userId+"/intent/"+amt, 
            {method:"GET", headers:{"Content-Type":"application/json"}}
        ).then(function(result) {
            return result.json();
        }).then(function(data) {
            curr.paymentIntent = data;
            if(data.payment_method_types)
            {
                if(data.payment_method_types.length>1)
                {
                    curr.draft.activePage = secid+'-paylist';
                    curr.render();
                    //curr.showPaymentMethod(2, secid);
                }
                else
                {
                    curr.showPaymentMethod(0, secid);
                }
            }
            else
            {
                const cardDisplay = doc.getElementById('ff-card-element-'+secid);
                cardDisplay.innerHTML = '<b style="color:red">Payment not configured correctly</b>';
                const displayError = doc.getElementById('ff-card-errors-'+secid);
                displayError.innerHTML = data.error?data.error:'Unknown error';
            }
        });
    }

    this.showPaymentMethod = function(idx, secid)
    {
        var curr = this;
        var doc = this.getDocument();
        this.draft.activePage = secid+'-pay';
        this.render();
        var data = this.paymentIntent;
        var meth = data.payment_method_types[idx];
        paymentIntentClientSecret = data.clientSecret;
        var displayError = doc.getElementById('ff-card-errors-'+secid);
        var stripe = Stripe(data.publishableKey, {stripeAccount:data.accountID});
        var methopts = {payment_method:{billing_details:{}}};
        if(curr.data.facade.mapping)
        {
            var keyfields = {};
            ['name', 'email', 'phone', 'address'].forEach(attr=>{
                var iid = curr.data.facade.mapping[attr];
                var itm = iid?curr.data.scraped.items[iid]:null;
                keyfields[attr] = itm?curr.draft.entry[itm.entry]:null;
            });
            var {name, email, phone, address} = keyfields;
            //methopts.receipt_email = email;
            methopts.payment_method.billing_details = {name, email, phone};
            var track = curr.draft.savedId+'*'+curr.draft.draftSeq;
            methopts.shipping = {tracking_number:track, name:name||'-', address:{line1:address||'-'}};
        }
        var card;
        if(meth.mount)
        {
            var elements = stripe.elements();
            var style = {base:{color: "#32325d"}};
            card = elements.create(meth.mount, {style:style});
            card.mount("#ff-card-element-"+secid);
            card.on('change', ({error}) => {
              if(error) {
                displayError.textContent = error.message;
              } else {
                displayError.textContent = '';
              }
            });
            methopts.payment_method[meth.id] = card;
        }
        else
        {
            if(!methopts.payment_method.billing_details.name)
                methopts.payment_method.billing_details.name = 'neartail';
            var cardelm = doc.getElementById('ff-card-element-'+secid);
            if(cardelm) cardelm.style.display = 'none';
        }
        var payform = doc.getElementById('ff-payment-form-'+secid);
        payform.addEventListener('submit', function(ev) {
          ev.preventDefault();
          displayError.textContent = '';
          doc.querySelectorAll('#ff-pay-'+secid+' img').forEach(function(elm){
            elm.src = 'https://formfacade.com/img/loading.svg';
          });
          if(meth.flow=='redirect')
          {
            methopts.return_url = location.href.split('?')[0];
            curr.createCookie('ff-payment-section', secid, 1/24);
            curr.createCookie('ff-payment_intent_client_secret', paymentIntentClientSecret, 1/24);
            curr.draft.activePage = secid;
            curr.saveDraft();
          }
          stripe[meth.invoke](data.clientSecret, methopts).then(function(result){
            if(result.error)
            {
              displayError.innerHTML = result.error.message;
              doc.querySelectorAll('#ff-pay-'+secid+' img').forEach(function(elm){
                elm.src = 'https://formfacade.com/img/send.svg'; 
              });
            }
            else
            {
              if(result.paymentIntent && result.paymentIntent.status==='succeeded') 
              {
                var payelm = doc.getElementById('Payment'+curr.data.request.params.publishId);
                if(payelm) payelm.value = result.paymentIntent.id;
                var rfrm = doc.getElementById('Publish'+curr.data.request.params.publishId);
                curr.draft.activePage = null;
                doc.querySelectorAll('#ff-pay-'+secid+' img').forEach(function(elm){
                    elm.src = 'https://formfacade.com/img/loading.svg'; 
                });
                curr.submit(rfrm, secid);
              }
            }
          });
        });
    }
    
    this.showNavigation = function()
    {
        var overlay = document.getElementById('ff-addprd-overlay');
        overlay.classList.add('active');
        var popup = document.getElementById('ff-addprd-popup');
        popup.classList.add('active');
        popup.innerHTML = ejs.render(this.template.navigation, this);
        document.body.style.overflowY = 'hidden';
    }

    this.closeNavigation = function()
    {
        document.body.style.overflowY = 'auto';
        var overlay = document.getElementById('ff-addprd-overlay');
        overlay.classList.remove('active');
        var popup = document.getElementById('ff-addprd-popup');
        popup.classList.remove('active');
    }

    this.checkInventory = function(iid)
    {
        var curr = this;
        var fac = this.data.facade||{};
        var facitms = fac.items||{};
        var facitm = facitms[iid]||{};
        if(facitm.inventory=='yes' || facitm.measure === 'Nested')
        {
            var {publishId} = curr.data.request.params;
            return fetch('https://cache.formfacade.com/data/facade/'+publishId+'-editable')
            .then(req=>req.json()).then(chfac=>{
                var chitm = chfac.items[iid]||{};
                curr.data.facade = chfac;
                curr.getSections(true);
                if (facitm.measure === 'Nested')
                {
                    var vrns = Object.keys(facitm.variants || {});
                    var vrns1 = Object.keys(facitm.variants1 || {});
                    for(let i = 0; i < vrns.length; i++)
                    {
                        var vid = vrns[i];
                        for(let j = 0; j < vrns1.length; j++)
                        {
                            var vid1 = vrns1[j];
                            var variantConfig = chitm.variantConfig || {};
                            var inventory = variantConfig.inventory || {};
                            var inv = inventory[vid + "-" + vid1] || {};
                            var remain = inv.remain;
                            if(inv.remain === undefined) {
                                remain = "";
                            }
                            var currRemain = "";
                            var currVariantConfig = facitm.variantConfig || {};
                            var currInventory = currVariantConfig.inventory || {};
                            var currInv = currInventory[vid + "-" + vid1] || {};
                            currRemain = currInv.remain;
                            if(currRemain === undefined) {
                                currRemain = "";
                            }
                            if(remain === currRemain) {
                                continue;
                            }
                            if(remain < currRemain) return true;
                        }
                    }
                }
                else if(facitm.measure=='Configurable' && facitm.variants)
                {
                    var vrns = facitm.variants||{};
                    for(var vid in vrns)
                    {
                        var vrn = vrns[vid];
                        var chvrn = chitm.variants[vid];
                        if(chvrn.remain<vrn.remain) return true;
                    }
                }
                else if(facitm.measure=='Quantity' || facitm.measure=='Weight')
                {
                    return chitm.remain<facitm.remain;
                }
            });
        }
        else
        {
            return Promise.resolve(false);
        }
    }

    this.showProduct = function(iid, crrtab, idx=0, opts = {})
    {
        var curr = this;
        this.product = {id:iid};
        if (opts && opts.hideShowTable) {
            curr.hideShowTable = true;
        }
        else {
            curr.hideShowTable = false;
        }
        if(opts.vid && opts.v1id) {
            curr.cartSelected = opts;
        } else {
            curr.cartSelected = null;
        }
        var item = this.data.scraped.items[iid];
        var fcitm = this.data.facade.items[iid] || {};
        if(item && item.type=='PARAGRAPH_TEXT')
        {
            var val = this.draft.entry?this.draft.entry[item.entry]:null;
            this.product.configurable = this.toConfigurable(val);
            this.product.configurable.index = idx;
            var ci = this.product.configurable.configItem[idx];
            this.product.configItem = ci||{lineItem:{}, selected:null, page:'variant'};
            this.product.configurable.configItem[idx] = this.product.configItem;
        }
        var overlay = document.getElementById('ff-addprd-overlay');
        overlay.classList.add('active');
        var popup = document.getElementById('ff-addprd-popup');
        popup.classList.add('active');
        if(item && this.draft.entry[item.entry]) {
            crrtab = 2;
        }
        this.initialActiveTab = crrtab === 2 ? 'cart' : "description";
        this.params = {};
        if(this.data && this.data.request && this.data.request.params) {
            this.params = this.data.request.params;
        }
        popup.innerHTML = ejs.render(this.template.product, this);
        document.body.style.overflowY = 'hidden';
        this.checkInventory(iid).then(changed=>{
            if(changed) curr.showProduct(iid);
        });
    }

    this.addProduct = function(enid, val, close)
    {
        var vals = this.draft.entry[enid];
        vals = vals?(Array.isArray(vals)?vals:[vals]):[];
        vals = vals.concat(val);
        this.updateProduct(enid, vals, close);
    }

    this.removeProduct = function(enid, val, close)
    {
        var vals = this.draft.entry[enid];
        vals = vals?(Array.isArray(vals)?vals:[vals]):[];
        const idx = vals.indexOf(val);
        if(idx > -1) vals.splice(idx, 1);
        this.updateProduct(enid, vals, close);
    }

    this.getModifiers = function(id)
    {
        var fcitem = this.data.facade.items[id]||{};
        var mods = fcitem.modifiers||{};
        var modlst = Object.entries(mods).map(mod=>{
            var [id, val] = mod;
            return Object.assign({id}, val);
        });
        modlst.sort((a,b)=>a.index-b.index);
        return modlst;
    }
    
    this.skipQuantityForModifiers = function (enid, val = 1)
    {
        if (enid) 
        {
            if (val)
                this.draft.entry[enid] = val;
            else
                delete this.draft.entry[enid];
        }
        var id = this.product.id;
        var fcitem = this.data.facade.items[id] || {};
        if (fcitem.modifiers) {
            var mods = this.getModifiers(id);
            var mod = mods[0] || {};
            if (mod.id) {
                if (this.draft.entry[enid]) {
                    this.showProduct(mod.id, 2, 0);
                }
                else
                    this.renderProduct();
            }
            else
                this.closePopup();
        }
    }

    this.updateProduct = function(enid, val, close)
    {
        if(enid)
        {
            if(val)
                this.draft.entry[enid] = val;
            else
                delete this.draft.entry[enid];
        }
        if(close)
        {
            var id = this.product.id;
            var fcitem = this.data.facade.items[id]||{};
            if(fcitem.modifiers)
            {
                var mods = this.getModifiers(id);
                var mod = mods[0]||{};
                if(mod.id)
                {
                    if(this.draft.entry[enid])
                        this.showProduct(mod.id, 2);
                    else
                        this.renderProduct();
                }
                else
                    this.closePopup();
            }
            else if (fcitem.measure === 'Nested' && fcitem.multiselect === 'yes')
            {
                formFacade.render();
                this.showProduct(id, 2);
            }
            else
            {
                this.closePopup();
            }
        }
        else
        {
            this.renderProduct();
        }
    }

    this.showModifierTable = function (id, tab)
    {
        formFacade.render();
        formFacade.saveDraft();
        this.showProduct(id, tab);
    }

    this.renderProduct = function(vid)
    {
        var popup = document.getElementById('ff-addprd-popup');
        this.initialActiveTab = 'cart';
        popup.innerHTML = ejs.render(this.template.product, this);
        const prdContent = document && document.querySelector('#prdtab-cart');
        if(prdContent) {
            prdContent.scrollIntoView({
                behavior: "instant"
            });
        }
    }

    this.submitConsent = function() {
        var {userId, publishId} = this.data.request.params;
        this.consentAgreed = true;
        var accepted = document.getElementById(`Accepted${publishId}`);
        var acceptedAt = document.getElementById(`AcceptedAt${publishId}`);
        if(accepted) accepted.value = this.consentSecId;
        if(acceptedAt) acceptedAt.value = new Date().getTime();
        
        this.submit(document.getElementById(`Publish${publishId}`), this.consentSecId)
        this.closePopup(false);
    }

    this.closeConsent = function()
    {
        var elms = document.querySelectorAll(`#ff-submit-${this.consentSecId} img`);
        elms.forEach(function(elm){ 
            elm.src = 'https://formfacade.com/img/send.svg'; 
        });
        this.closePopup(false);
    }

    this.consentDialog = function(submitSec) {
        var {userId, publishId} = this.data.request.params;
        var baseurl = 'https://formfacade.com';
        if(this.data.devEnv)
            baseurl = 'http://localhost:5000';
    
        var curr = this;
        var fc = this.data.facade || {};
        var scItems = this.data.scraped.items || {};
        var isDefaultConsent = (submitSec.template == null);

        if(isDefaultConsent) {
            curr.addLinkTag('/js/mailrecipe/kanban.css');
            var {savedId} = this.draft||{};
            var savedId = curr.draft.savedId;
            if(!savedId) savedId = curr.readCookie('ff-'+publishId);
            var prm = Promise.resolve(savedId);
            if(!savedId) prm = curr.saveDraft().then(_=>curr.draft.savedId);

            prm.then(function(svid){
                if(!svid) throw Error('Save failed! Try again.');
                fetch(`${baseurl}/draft/${publishId}/summary/${savedId}`, { method: "GET"}).then(function(result) {
                    return result.text();
                }).then(function(data) {
                    var html =  `<div class="ff-consent-start ff-consent-default"> ${data} </div>`
                    curr.showConsentDialog(html);
                });
            });
            
        } else {
            fetch(`${baseurl}/consent/docs/${submitSec.template}`, { method: "GET"})
            .then(req=>req.json()).then(jso=>jso||{})
            .then(function(jso) {
                var html = curr.computeField(jso.html)
                curr.showConsentDialog(html);
                if(jso.margin)
                    $(".ff-consent-start").css({padding: `${jso.margin.top} ${jso.margin.right} ${jso.margin.bottom} ${jso.margin.left}`});
            });
        }
    }

    this.getDateObject = function(ds)
    {
        var time = null;
        if (ds.includes(' ')) {
            [ds, time] = ds.split(' ');
        }
        var parts = ds.split('/');
        var month = Number(parts[1]) - 1;
        var dt = new Date(parts[2], month, parts[0]);
        if(time) {
            var [hour, minute] = time.split(':');
            dt.setHours(hour);
            dt.setMinutes(minute);
        }
        return dt;
    }

    this.getMonth = function(ds)
    {
        var dt = this.getDateObject(ds);
        return dt.toLocaleString('default', { month: 'long' });
    }

    this.getDay = function(ds)
    {
        var dt = this.getDateObject(ds);
        // return monday, tuesday, etc
        return dt.toLocaleString('default', { weekday: 'long' });
    }

    this.getDate = function(ds)
    {
        var dt = this.getDateObject(ds);
        return dt.getDate();
    }

    this.getYear = function(ds)
    {
        var dt = this.getDateObject(ds);
        return dt.getFullYear();
    }

    this.getFormattedTime = function (itemId, dateString) 
    {
        var dateObj = this.getDateObject(dateString);
        var format = "12"; // Default format
        if (this.data.facade.items[itemId].variantConfig.timeFormat) {
            format = this.data.facade.items[itemId].variantConfig.timeFormat;
        }
        if (format === "24") {
            // 24-hour format
            var hours = dateObj.getHours();
            var minutes = dateObj.getMinutes();
            hours = hours < 10 ? '0' + hours : hours;
            minutes = minutes < 10 ? '0' + minutes : minutes;
            var formattedTime = hours + ':' + minutes;
            return formattedTime;
        }

        var hours = dateObj.getHours();
        var minutes = dateObj.getMinutes();
        var ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12; // the hour '0' should be '12'
        minutes = minutes < 10 ? '0' + minutes : minutes;
        var formattedTime = hours + ':' + minutes + ' ' + ampm;
        return formattedTime;
    }

    this.getFormattedDate = function(itemId, dateString, time)
    {
        var dateObj = this.getDateObject(dateString);
        var format = "DD/MM/YYYY"; // Default format
        var separator = "/"; // Default separator

        if (this.data.facade.items[itemId].variantConfig.dateFormat) {
            format = this.data.facade.items[itemId].variantConfig.dateFormat;
        }
        if (this.data.facade.items[itemId].variantConfig.dateSeparator) {
            separator = this.data.facade.items[itemId].variantConfig.dateSeparator;
        }

        var day = dateObj.getDate();
        var month = dateObj.getMonth() + 1; // Months are zero-based
        var year = dateObj.getFullYear();
        var hours = dateObj.getHours();
        var minutes = dateObj.getMinutes();

        day = day < 10 ? '0' + day : day;
        month = month < 10 ? '0' + month : month;
        hours = hours < 10 ? '0' + hours : hours;
        minutes = minutes < 10 ? '0' + minutes : minutes;

        var formattedDate = "";
        if (format === "DD/MM/YYYY") {
            formattedDate = day + separator + month + separator + year;
        } else if (format === "MM/DD/YYYY") {
            formattedDate = month + separator + day + separator + year;
        } else if (format === "YYYY/MM/DD") {
            formattedDate = year + separator + month + separator + day;
        } else if (format === "YYYY/DD/MM") {
            formattedDate = year + separator + day + separator + month;
        }

        if (time) {
            formattedDate += " " + this.getFormattedTime(itemId, dateString);
        }

        return formattedDate;
    }

    this.showConsentDialog = function(data) {
        var overlay = document.getElementById('ff-addprd-overlay');
        overlay.classList.add('active');
        var popup = document.getElementById('ff-addprd-popup');
        popup.classList.add('active');
        popup.classList.add('ff-consent-confirm');
        popup.innerHTML = `
            <div class="ff-consent-content"> <div class="ff-consent-body">${data} </div> </div>
            <div class="ff-consent-footer ff-form">
                <button type="button" class="rest-btn rest-btn-lg ff-submit" onClick="formFacade.submitConsent()" >Agree</button>
                <button type="button" class="rest-btn rest-btn-lg ff-consent-close" onClick="formFacade.closeConsent()">Close</button>
            </div>
        `;

    }

    this.weeklyDateValidation = function (id, e)
    {
        var fcitm = this.data.facade.items[id] || {};
        var validation = fcitm.validation || {};
        var validValue = validation.validDays ? validation.validDays.split(',') : '';
        if(!validValue.length) {
            return;
        }
        const value = e.value;
        const date = new Date(value);
        // Get monday, tuesday.
        const day = date.toLocaleString('default', { weekday: 'long' });
        if(!validValue.includes(day)) {
            document.querySelector(`#Error${id}`).innerHTML = `Invalid selection. Please choose a valid day: ${validation.validDays.split(",").join(", ")}`;
            document.querySelector(`#Error${id}`).style.display = 'block';
            e.value = '';
        } else {
            document.querySelector(`#Error${id}`).innerHTML = '';
            document.querySelector(`#Error${id}`).style.display = 'none';
        }
    }

    this.futureLeadDateValidation = function(id, e)
    {
        var fcitm = this.data.facade.items[id] || {};
        var validation = fcitm.validation || {};
        var leadDate = Number(fcitm.validValue || "0");
        var value = e.value;
        var date = new Date(value);
        var minDate = this.addDays(new Date(), parseInt(validation.validValue||0));
        if(date < minDate && leadDate) {
            document.querySelector(`#Error${id}`).innerHTML = `Invalid date selection. Please choose a date that is at least ${leadDate} days from today.`;
            document.querySelector(`#Error${id}`).style.display = 'block';
            e.value = '';
        } else {
            document.querySelector(`#Error${id}`).innerHTML = '';
            document.querySelector(`#Error${id}`).style.display = 'none';
        }
    }

    this.futureDateValidation = function(id, e)
    {
        var fcitm = this.data.facade.items[id] || {};
        var value = e.value;
        var date = new Date(value);
        var today = new Date();
        // dont compare time, set them as a same time.
        today.setHours(0, 0, 0, 0);
        date.setHours(0, 0, 0, 0);
        if(date < today) {
            document.querySelector(`#Error${id}`).innerHTML = `Invalid date selection. Please choose a future date.`;
            document.querySelector(`#Error${id}`).style.display = 'block';
            e.value = '';
        } else {
            document.querySelector(`#Error${id}`).innerHTML = '';
            document.querySelector(`#Error${id}`).style.display = 'none';
        }
    }

    this.excludeDateValidation = function (id, e)
    {
        var fcitm = this.data.facade.items[id] || {};
        var validation = fcitm.validation || {};
        var validValue = validation.excludedDates ? validation.excludedDates.split(',') : '';
        if(!validValue.length) {
            return;
        }
        // validValue will be YYYY-MM-DD
        const value = e.value;
        if(validValue.includes(value)) {
            document.querySelector(`#Error${id}`).innerHTML =  `Invalid date selection. The selected date is not allowed. Please choose a different date.`;
            document.querySelector(`#Error${id}`).style.display = 'block';
            e.value = '';
        } else {
            document.querySelector(`#Error${id}`).innerHTML = '';
            document.querySelector(`#Error${id}`).style.display = 'none';
        }
    }

    this.toConfigurable = function(str='')
    {
        var curr = this;
        var configurable = {configItem:[], index:0};
        configurable.configItem = str.split('\n-----\n').map(val=>curr.toConfigItem(val));
        configurable.toValue = function(fcitm){
            return this.configItem.map(ci=>{
                var lns = [];
                for(var vid in ci.lineItem)
                {
                    if(vid.includes('-')) {
                        var [v, v1] = vid.split('-');
                        var qty = ci.lineItem[vid];
                        var vmap = fcitm.variants||{};
                        var vmeta = vmap[v]||{};
                        var nm = vmeta.name||'-';
                        var vmap1 = fcitm.variants1||{};
                        var vmeta1 = vmap1[v1]||{};
                        var nm1 = vmeta1.name||'-';
                        lns.push(nm+' | '+v+' || '+nm1+' | '+v1+' * '+qty);
                        continue;
                    }
                    var qty = ci.lineItem[vid];
                    var vmap = fcitm.variants||{};
                    var vmeta = vmap[vid]||{};
                    var nm = vmeta.name||'-';
                    lns.push(nm+' | '+vid+' * '+qty);
                }
                return lns.join('\n');
            }).join('\n-----\n');
        }
        return configurable;
    }

    this.toConfigItem = function(val)
    {
        var configItem = {lineItem:{}, selected:null, page:'variant'};
        var rws = val?val.split('\n'):[];
        rws.forEach(rw => {
            if (rw && rw.trim && rw.trim().includes('||')) {
                var [v, ln1] = rw.trim().split(' || ');
                var [v1, qstr] = ln1.trim().split(' * ');
                var vid = v.split(' | ').pop();
                var vid1 = v1.split(' | ').pop();
                var qty = isNaN(qstr) ? 0 : parseFloat(qstr);
                if (qty > 0) {
                    configItem.lineItem[`${vid}-${vid1}`] = qty;
                }
            } else {
                var ln = rw.trim().split(' | ').pop();
                var [vid, qstr] = ln.trim().split(' * ');
                var qty = isNaN(qstr)?0:parseFloat(qstr);
                if(qty>0) configItem.lineItem[vid] = qty;
            }
        });
        return configItem;
    }

    this.selectVariant = function(vid, page = 'quantity')
    {
        var cfg = this.product.configItem;
        cfg.selected = vid;
        cfg.page = page;
        this.renderProduct();
    }

    this.selectVariant1 = function(vid, v1id)
    {
        var cfg = this.product.configItem;
        cfg.selected = vid;
        cfg.selected1 = v1id;
        cfg.page = 'quantity';
        this.renderProduct();
    }

    this.enterQuantity = function(value, id, vid, v1id, max)
    {
        if(Number(value) > Number(max)) {
            document.querySelector('.ff-cart-error-message').innerHTML = `Quantity should be less than or equal to ${max}`;
            return document.querySelector('.ff-nested-add-to-cart').style.display = 'none';
        }
        document.querySelector('.ff-cart-error-message').innerHTML = '';
        return document.querySelector('.ff-nested-add-to-cart').style.display = 'inline';
    }

    this.addNewModifier = function(enid, mid, val)
    {
        if (enid) {
            if (val)
                this.draft.entry[enid] = `${(Number(val) + 1)}`;
            else
                delete this.draft.entry[enid];
        }
        this.product.configurable = this.toConfigurable(this.draft.entry[enid]);
        this.product.configurable.index = val;
        var ci = this.product.configurable.configItem[val];
        this.product.configItem = ci || { lineItem: {}, selected: null, page: 'variant' };
        this.product.configurable.configItem[val] = this.product.configItem;
        formFacade.showProduct(mid, 2, val)
    }

    this.updateQuantity = function(vid, qty, close)
    {
        var item = this.data.scraped.items[this.product.id];
        var fcitm = this.data.facade.items[this.product.id];
        var cfg = this.product.configItem;
        if(fcitm.measure === 'Nested' && fcitm.multiselect !== 'yes') {
            formFacade.product.configItem.lineItem={};
        }
        if(qty)
            cfg.lineItem[vid] = qty;
        else
            delete cfg.lineItem[vid];
        var cfgval = this.product.configurable.toValue(fcitm);
        if(close)
        {
            if(fcitm.multiselect=='yes' && fcitm.measure === 'Nested')
            {
                this.updateProduct(item.entry, cfgval, true);
            } else 
            {
                this.updateProduct(item.entry, cfgval, close);
                var mod = this.data.facade.items[this.product.id];
                var mods = this.getModifiers();
                var mod = mods[0]||{};
                if(mod.id) 
                    this.showProduct(mod.id);
                else
                    this.closePopup();
            }
        }
        else
        {
            if(fcitm.multiselect=='yes'||fcitm.modifierfor)
            {
                if(cfgval)
                    this.draft.entry[item.entry] = cfgval;
                else
                    delete this.draft.entry[item.entry];
                this.product.configItem.page = 'variant';
                this.renderProduct();

                if(fcitm.modifierfor)
                {
                    setTimeout(() => {
                        document.getElementById(`li.${vid}`).scrollIntoView({
                            behavior: "instant"
                        });
                    }, 5);
                }
                
            }
            else
            {
                this.updateProduct(item.entry, cfgval, true);
            }
        }
    }

    this.cancelComboProductSelection = function (id, val, index) 
    {
        var item = this.data.scraped.items[id] || {};
        var lines = this.calculateEngine("${getBill()}", { returntype: true });
        let _ = {};
        for (let i = 0; i < lines.length; i++) {
            var line = lines[i] || [];
            if (line && line[8] == id) {
                if (_[line[9]]) {
                    _[line[9]].push(line);
                } else {
                    _[line[9]] = [line];
                }
            }
        }
        if(Object.keys(_).length <= index)
        {
            this.draft.entry[item.entry] -= 1;
        }
        if (this.draft.entry[item.entry] == 0) {
            return this.showProduct(id, 2);
        } else {
            return this.showProduct(id, 2);
        }
    }

    this.deleteCombo = function (id, index) 
    {
        var curr = this;
        var item = this.data.scraped.items[id] || {};
        if (index || index >= 0)
            this.draft.entry[item.entry] -= 1;
        else
            delete this.draft.entry[item.entry];
        this.getModifiers(id).forEach(mod => {
            var moditem = curr.data.scraped.items[mod.id] || {};
            if (index && curr.draft.entry[moditem.entry] && curr.draft.entry[moditem.entry].split('\n-----\n')) {
                var lines = curr.draft.entry[moditem.entry].split('\n-----\n');
                lines.splice(index, 1);
                curr.draft.entry[moditem.entry] = lines.join('\n-----\n');
            } else {
                delete curr.draft.entry[moditem.entry];
            }
        });
        this.closePopup();
        setTimeout(() => {
            curr.showProduct(id, 2);
        }, 10);
    }

    this.showPopup = function(header, content, footer, opts={})
    {
        var overlay = document.getElementById('ff-addprd-overlay');
        overlay.classList.add('active');
        var popup = document.getElementById('ff-addprd-popup');
        popup.classList.add('active');
        popup.innerHTML = `
            <div class="ff-popup-header ff-error-popup">
                <span class="ff-popup-title">${header}</span>
                <span class="material-icons" onclick="formFacade.closePopup(${opts.render?true:false})">close</span>
            </div>
            <div class='ff-error-popup'>
                <div class="ff-popup-content">
                    ${content}
                </div>
                <div class="ff-popup-footer">
                    ${footer||''}
                </div>
            </div>
        `;
        document.body.style.overflowY = 'hidden';
    }


    this.showWarning = function(title, err, footer, opts={})
    {
        var message = title;
        if(err)
        {
            if(typeof err==='string'||err instanceof String)
                message = err;
            else if(err instanceof Error)
                message = err.message;
            else
                message = JSON.stringify(err);
        }
        if(opts.ignorePopup != true){
            this.showPopup(
                `${title}`, 
                `<span style="color:red">âš  ${message}</span>`,
                footer
            );
        }
        
        var {userId, publishId} = this.data.request.params;
        var {savedId} = this.draft||{};
        if(publishId && savedId)
        {
            var {form, title:formtitle} = this.data.scraped||{};
            const payload = new URLSearchParams();
            payload.append('title', title);
            payload.append('message', message);
            payload.append('form', form||formtitle);
            payload.append('critical', opts.critical);
            var baseurl = this.data.devEnv?'http://localhost:5000':'https://formfacade.com';
            var fetchurl = `${baseurl}/draft/${userId}/form/${publishId}/error/${savedId}`;
            fetch(fetchurl, {method:'POST', body:payload}).then(req=>req.json());
        }
    }

    this.showError = function(title, err, footer, opts={})
    {
        opts.critical = 1;
        this.showWarning(title, err, footer, opts);
    }

    this.togglePrdTabPopup = function(activeTab)
    {
        const accprdtabContent = document.getElementById('prdtab-'+activeTab);
        const accprdtabHeader = document.getElementById('prdtab-'+activeTab+'-header');
        
        // Remove active from all the tab headers
        document.querySelectorAll('.prdtab-header').forEach(item => {
            item.classList.remove('prdtab-active-header');
        });

        // Remove active from all the tab contents
        document.querySelectorAll('.prdtab-content').forEach(item => {
            item.classList.remove('prdtab-active');
        });

        accprdtabContent.classList.add('prdtab-active');
        accprdtabHeader.classList.add('prdtab-active-header');
    }

    this.closePopup = function(render=true)
    {
        document.body.style.overflowY = 'auto';
        var overlay = document.getElementById('ff-addprd-overlay');
        overlay.classList.remove('active');
        var popup = document.getElementById('ff-addprd-popup');
        popup.classList.remove('active');
        popup.classList.remove('ff-consent-confirm');
        setTimeout(function(){
            if(render) formFacade.render();
            formFacade.saveDraft();
            if(window.customizeTemplate)
                window.customizeTemplate.renderFacadeInSinglePage()
        }, 10);
    }

    this.slugify = function(string) 
    {
        if(!string) return '';
        return string.toString().trim().toLowerCase()
        .replace(/\s+/g, "-").replace(/[^\w\-]+/g, "").replace(/\-\-+/g, "-").replace(/^-+/, "").replace(/-+$/, "");
    }

    this.paddingWithZero = function(number, size = 5) {
        number = parseInt(number)
        if (number.toString().length > size) return number;
        let s = "0000000" + number;
        return "ORD" + s.substr(s.length - size);
    }

    this.encode = function(text) {
        if(!text) return '';
        if(typeof window !== 'undefined')
            return window.btoa(text);
        else
            return Buffer.from(text).toString('base64');
    }

    this.getMinMaxDate = function(item, fitem) {
        var minDate = "1900-01-01";
        var maxDate = "2200-01-01";
        var date = new Date();
        var curDate = this.formatDate(new Date(), 'yyyy-MM-dd')
        var validation = fitem.validation
        if(validation && validation.validType == 'Date') {
            if(validation.validOperator == "Past")
                maxDate = curDate;
            else if(validation.validOperator == "Future")
                minDate = curDate;
            else if(validation.validOperator == "FutureLead") {
                var leadDate = this.addDays(new Date(), parseInt(validation.validValue||0));
                leadDate = this.formatDate(leadDate, 'yyyy-MM-dd')
                minDate = leadDate;
            } else if(validation.validOperator == "Between") {
                if(validation.validValue && validation.validValue.length == 10)
                    minDate = this.formatDate(new Date(validation.validValue), 'yyyy-MM-dd')
                if(validation.validValue2 && validation.validValue2.length == 10)
                    maxDate = this.formatDate(new Date(validation.validValue2), 'yyyy-MM-dd')
            }
        }
        if(item.time==1) {
            minDate += "T01:01";
            maxDate += "T23:59";
        }
        return [minDate, maxDate];
    }

    this.formatDate = function (x, y) {
        var z = { M: x.getMonth() + 1, d: x.getDate(), h: x.getHours(), m: x.getMinutes(), s: x.getSeconds() };
        y = y.replace(/(M+|d+|h+|m+|s+)/g, function(v) {
            return ((v.length > 1 ? "0" : "") + z[v.slice(-1)]).slice(-2)
        });
        return y.replace(/(y+)/g, function(v) {
            return x.getFullYear().toString().slice(-v.length)
        });
    }

    this.addDays = function (date, days) {
        var result = new Date(date);
        result.setDate(result.getDate() + days);
        return result;
    }

    this.switchCDN = function(url)
    {
        return url;
    }

    this.switchAllCDN = function(text)
    {
        if(text && text.indexOf('https://cdn.neartail.com/') > -1) {
            return text.replaceAll('https://cdn.neartail.com/', 'https://cdn.formfacade.com/');
        } else {
            return text;
        }
    }
}


FormFacade.prototype.getExpiry = function(yyyymmdd)
	{
		if(yyyymmdd)
		{
			var yyyy = Math.floor(yyyymmdd/10000);
			var mmdd = yyyymmdd - yyyy*10000;
			var mm = Math.floor(mmdd/100);
			var dd = mmdd - mm*100;
			var expdt = new Date(yyyy, mm-1, dd);
			return expdt;
		}
		return null;
	}



FormFacade.prototype.isPaid = function(userId, usr)
	{
		if(usr && usr.paid)
		{
			if(usr.paid.expires)
			{
				var expdt = this.getExpiry(usr.paid.expires);
				if(new Date().getTime() > expdt.getTime())
					return false;
			}
			return true;
		}
		else
			return false;
	}



FormFacade.prototype.asSections = function(frm)
{
    var sitems = [];
    frm = frm?frm:{};
    var itms = frm.items?frm.items:{};
    for(var sid in itms)
    {
        var sitm = itms[sid];
        sitm.id = sid;
        sitems.push(sitm);
    }
    sitems.sort(function(a,b){ return a.index-b.index; });
    var section = {
        id:'root', items:[], headers:[],
        title:frm.title, description:frm.description,
        titleMark:frm.titleMark, helpMark:frm.helpMark
    };
    var sections = [section];
    sitems.forEach(function(sitem){
        if(sitem.type=='PAGE_BREAK')
        {
            sections[sections.length-1].next = sitem.navigateTo;
            section = {
                title:sitem.title, id:sitem.id, headers:[], items:[],
                titleMark:sitem.titleMark, helpMark:sitem.helpMark
            };
            if(sitem.help) section.description = sitem.help;
            sections.push(section);
        }
        else
        {
            sitem.section = section;
            section.items.push(sitem);
        }
    });
    sections.forEach((sec, s)=>{
        var header = {items:[]};
        sec.headers.push(header);
        sec.items.forEach(function(item, itmi){
            if(item.deleted) {

            }
            else if(item.type=='SECTION_HEADER')
            {
                header = {head:item, items:[]};
                sec.headers.push(header);
            }
            else
            {
                if(item.choices)
                {
                    var shortchs = item.choices.filter(ch=>String(ch.value||'').length<40);
                    item.wrap = item.choices.length>=8&&item.choices.length==shortchs.length;
                }
                header.items.push(item);
            }
        });
    });
    return sections;
}



FormFacade.prototype.validateEngine = function(itm, frmdata, reportError)
{
    var curr = this;
    var txtval = frmdata.get('entry.'+itm.entry);
    if(!itm.validType && itm.overwrite && itm.overwrite.validation && itm.overwrite.validation.validType) {
        Object.assign(itm, itm.overwrite.validation);
    }
	if(itm.type=='CHECKBOX')
    {
        var valarr = frmdata.getAll('entry.'+itm.entry);
        var valothr = frmdata.get('entry.'+itm.entry+'.other_option_response');
        var validothr = valothr?!valothr.trim():true;
        var validop = itm.validOperator;
        var validval = itm.validValue;
        if(isNaN(validval)==false)
            validval = parseInt(validval);
        var validmsg = itm.validMessage;
        if(itm.required && valarr.length==0)
        {
            reportError(curr.lang('Please fill this'));
        }
        else if(itm.required && valarr.length==1 && valarr[0]=='__other_option__' && validothr)
        {
            reportError(curr.lang('Please fill this'));
        }
        else if(validop=='Atmost' && valarr.length>validval)
        {
            if(!validmsg) validmsg = 'Must select at most '+validval+' options';
            reportError(validmsg);
        }
        else if(validop=='Atleast' && valarr.length<validval)
        {
            if(!validmsg) validmsg = 'Must select at least '+validval+' options';
            reportError(validmsg);
        }
        else if(validop=='Exactly' && valarr.length!=validval)
        {
            if(!validmsg) validmsg = 'Must select exactly '+validval+' options';
            reportError(validmsg);
        }
    }
    else if(itm.type=='MULTIPLE_CHOICE')
    {
        var valothr = frmdata.get('entry.'+itm.entry+'.other_option_response');
        var validothr = valothr?!valothr.trim():true;
        if(itm.required && txtval=='__other_option__' && validothr)
        {
            reportError(curr.lang('Please fill this'));
        }
    }
    else if(itm.type=='GRID')
    {
        if(itm.required)
        {
            itm.rows.forEach(function(rw, r){
                var valarr = frmdata.getAll('entry.'+rw.entry);
                if(valarr.length==0)
                {
                    validmsg = 'This question requires one response per row';
                    if(rw.multiple==1)
                        validmsg = 'This question requires at least one response per row';
                    validmsg = curr.lang(validmsg);
                    reportError(validmsg);
                }
            });
        }
        if(itm.onepercol)
        {
            var rwvals = {};
            itm.rows.forEach(function(rw, r){
                frmdata.getAll('entry.'+rw.entry).forEach(function(rwval){
                    if(rwvals[rwval])
                    {
                        validmsg = 'Please don\'t select more than one response per column';
                        validmsg = curr.lang(validmsg);
                        reportError(validmsg);
                    }
                    rwvals[rwval] = rw.entry;
                });
            });
        }
    }
    else if(itm.overwrite && itm.overwrite.type=='FILE_UPLOAD')
    {
        var fileval = frmdata.get('entry.'+itm.entry);
        var validmsg = itm.validMessage;
        if(itm.required && !fileval)
        {
            if(!validmsg) validmsg = curr.lang('Please fill this');
            reportError(validmsg);
        }
    }
    else if(txtval && (itm.type=='TEXT' ||  itm.type=='PARAGRAPH_TEXT'))
    {
        var validtyp = itm.validType;
        var validop = itm.validOperator;
        var validmsg = itm.validMessage;
        if(itm.validDynamic && itm.validEntryId)
        {
            var compTxtVal = frmdata.get('entry.'+itm.validEntryId);
            if(validtyp=='Number') {
                var compFltval; var fltval;
                if(isNaN(compTxtVal)==false)
                    compFltval = parseFloat(compTxtVal);
                if(isNaN(txtval)==false)
                    fltval = parseFloat(txtval);
                if(isNaN(txtval))
                    enmsg = 'Must be a number';
                else if(!compTxtVal || isNaN(compTxtVal))
                    enmsg = 'Comparison field must be a number';
                else if(validop=='GreaterThan' && fltval>compFltval==false)
                    enmsg = 'Must be a number greater than '+compFltval;
                else if(validop=='GreaterEqual' && fltval>=compFltval==false)
                    enmsg = 'Must be a number greater than or equal to '+compFltval;
                else if(validop=='LessThan' && fltval<compFltval==false)
                    enmsg = 'Must be a number less than '+compFltval;
                else if(validop=='LessEqual' && fltval<=compFltval==false)
                    enmsg = 'Must be a number less than or equal to '+compFltval;
                else if(validop=='EqualTo' && fltval!=compFltval)
                    enmsg = 'Must be a number equal to '+compFltval;
                else if(validop=='NotEqualTo' && fltval==compFltval)
                    enmsg = 'Must be a number not equal to '+compFltval;
                if(enmsg)
                {
                    reportError(validmsg?validmsg:enmsg);
                }
            } else if(validtyp=='Text') {
                var enmsg;
                var compTxtVal = frmdata.get('entry.'+itm.validEntryId);
                if(validop=='EqualTo' && txtval != compTxtVal)
                    enmsg = 'Must equal to '+itm.validValue;
                else if(validop=='NotEqualTo' && txtval == compTxtVal)
                    enmsg = 'Must not equal to '+itm.validValue;
                if(enmsg)
                {
                    reportError(validmsg?validmsg:enmsg);
                }

            }
        } else if(validtyp=='Number')
        {
            var enmsg;
            if(!itm.validValue)
                itm.validValue = 0;
            var fltval;
            if(isNaN(txtval)==false)
                fltval = parseFloat(txtval);
            var validval = itm.validValue;
            if(isNaN(validval)==false)
                validval = parseFloat(validval);
            if(isNaN(txtval))
                enmsg = 'Must be a number';
            else if(validop=='IsNumber' && isNaN(txtval))
                enmsg = 'Must be a number';
            else if(validop=='WholeNumber' && (isNaN(txtval) || txtval.indexOf('.')>=0))
                enmsg = 'Must be a whole number';
            else if(validop=='GreaterThan' && fltval>validval==false)
                enmsg = 'Must be a number greater than '+validval;
            else if(validop=='GreaterEqual' && fltval>=validval==false)
                enmsg = 'Must be a number greater than or equal to '+validval;
            else if(validop=='LessThan' && fltval<validval==false)
                enmsg = 'Must be a number less than '+validval;
            else if(validop=='LessEqual' && fltval<=validval==false)
                enmsg = 'Must be a number less than or equal to '+validval;
            else if(validop=='EqualTo' && fltval!=validval)
                enmsg = 'Must be a number equal to '+validval;
            else if(validop=='NotEqualTo' && fltval==validval)
                enmsg = 'Must be a number not equal to '+validval;
            else if(validop=='Between' && itm.validValue2 && (fltval<validval || fltval>parseFloat(itm.validValue2)))
                enmsg = 'Must be a number between '+itm.validValue+' and '+itm.validValue2;
            else if(validop=='NotBetween' && itm.validValue2 && (fltval>=validval && fltval<=parseFloat(itm.validValue2)))
                enmsg = 'Must be a number less than '+itm.validValue+' or greater than '+itm.validValue2;

            if(enmsg)
            {
                reportError(validmsg?validmsg:enmsg);
            }
        }
        else if(validtyp=='Text')
        {
            var enmsg;
            if(validop=='EqualTo' && txtval != itm.validValue)
                enmsg = 'Must equal to '+itm.validValue;
            else if(validop=='NotEqualTo' && txtval == itm.validValue)
                enmsg = 'Must not equal to '+itm.validValue;
            else if(validop=='Contains' && itm.validValue && (txtval.indexOf(itm.validValue)>=0)==false)
                enmsg = 'Must contain '+itm.validValue;
            else if(validop=='NotContains' && itm.validValue && (txtval.indexOf(itm.validValue)>=0))
                enmsg = 'Must not contain '+itm.validValue;
            else if(validop=='Email' && /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,63}$/.test(txtval)==false)
                enmsg = 'Must be an email';
            else if(validop=='URL' && /https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)/.test(txtval)==false)
                enmsg = 'Must be a URL';
            if(enmsg)
            {
                reportError(validmsg?validmsg:enmsg);
            }
        }
        else if(itm.validValue && validtyp=='Regex')
        {
            var enmsg;
            if(!txtval) txtval = '';
            var regx = new RegExp(itm.validValue, 'g');
            if(validop=='Contains' && regx.test(txtval)==false)
                enmsg = 'Must contain '+itm.validValue;
            else if(validop=='NotContains' && regx.test(txtval))
                enmsg = 'Must not contain '+itm.validValue;
            else if(validop=='Matches')
            {
            	var mtrs = txtval.match(regx);
            	var validmt = mtrs&&mtrs.length==1&&mtrs[0]==txtval;
            	if(!validmt) enmsg = 'Must match '+itm.validValue;
            }
            else if(validop=='NotMatches' && txtval.match(regx))
            {
            	var mtrs = txtval.match(regx);
            	var validmt = mtrs&&mtrs.length==1&&mtrs[0]==txtval;
            	if(validmt) enmsg = 'Must not match '+itm.validValue;
            }
            if(enmsg)
            {
                reportError(validmsg?validmsg:enmsg);
            }
        }
        else if(validtyp=='Length')
        {
            var enmsg;
            if(!itm.validValue)
                itm.validValue = 0;
            if(validop=='MaxChar' && txtval.length>parseInt(itm.validValue))
                enmsg = 'Must be fewer than '+itm.validValue+' characters';
            else if(validop=='MinChar' && txtval.length<parseInt(itm.validValue))
                enmsg = 'Must be at least '+itm.validValue+' characters';
            if(enmsg)
            {
                reportError(validmsg?validmsg:enmsg);
            }
        }
    }
}



FormFacade.prototype.calculateEngine = function(tmpl, opts={})
{
	var curr = this;
    var citm = opts.calcfield;
    var config = this.config||{};
    var request = this.data.request||{};
    var scraped = this.data.scraped||{};
    var items = scraped.items||{};
    var draft = this.draft||{};
    var entr = draft.entry||{};
    var fac = this.data.facade||{};
    var fcitms = fac.items||{};
    var setting = fac.setting||{};
    var defcurrency = setting.currency||'$';
    var params = {
        ALL:'__all__', VISIBLE:'__all__', FULL:'__full__', 
        SECTION:'__section__', CATEGORY:'__category__', SYMBOL:defcurrency
    };
    var asNumber = function(itm, val)
    {
        var vl = new Number(isNaN(val)?0:parseFloat(val));
        vl.getMetadata = function(){ return itm; }
        return vl;
    }
    var asString = function(itm, val)
    {
        var vl = new String(val||'');
        vl.getMetadata = function(){ return itm; }
        vl.valueOf = function()
        {
            var cformat = citm?citm.format:null;
            if((itm.format || cformat) && isNaN(val)==false)
                return Number(val);
            else
                return val;
        }
        return vl;
    }
    var asArray = function(itm, val)
    {
        var vl = val||[];
        vl.getMetadata = function(){ return itm; }
        return vl;
    }
    var toDate = function(dt)
    {
        var fill = function(nm){ return nm<10?('0'+nm):nm; }
        var val = function(vl){
            if(vl.getMetadata && vl.getMetadata().time==1)
                return vl.getFullYear()+'-'+fill(vl.getMonth()+1)+'-'+fill(vl.getDate())+'T'+fill(vl.getHours())+':'+fill(vl.getMinutes())+':'+fill(vl.getSeconds());
            else
                return vl.getFullYear()+'-'+fill(vl.getMonth()+1)+'-'+fill(vl.getDate());
        }
        if(citm) dt.getMetadata = function(){ return citm; };
        dt.valueOf = function(){ return this.getTime(); }
        dt.toString = function(){ return val(this); }
        dt.format = function(){ return params.formatDate(this); }
        dt.add = function(vl, dur){ 
            var tm = this.getTime();
            if(!dur || dur=='days')
                return toDate(new Date(tm+vl*24*60*60*1000));
            else if(dur=='months')
                return toDate(new Date(this.getFullYear(), this.getMonth()+vl, this.getDate(), this.getHours(), this.getMinutes(), this.getSeconds()));
            else if(dur=='years')
                return toDate(new Date(this.getFullYear()+vl, this.getMonth(), this.getDate(), this.getHours(), this.getMinutes(), this.getSeconds()));
            else if(dur=='hours')
                return toDate(new Date(this.getFullYear(), this.getMonth(), this.getDate(), this.getHours()+vl, this.getMinutes(), this.getSeconds()));
            else if(dur=='minutes')
                return toDate(new Date(this.getFullYear(), this.getMonth(), this.getDate(), this.getHours(), this.getMinutes()+vl, this.getSeconds()));
            else if(dur=='seconds')
                return toDate(new Date(this.getFullYear(), this.getMonth(), this.getDate(), this.getHours(), this.getMinutes(), this.getSeconds()+vl));
            return vl;
        }
        dt.subtract = function(vl, dur){ 
            var tm = this.getTime();
            if(!dur || dur=='days')
                return toDate(new Date(tm-vl*24*60*60*1000));
            else if(dur=='months')
                return toDate(new Date(this.getFullYear(), this.getMonth()-vl, this.getDate(), this.getHours(), this.getMinutes(), this.getSeconds()));
            else if(dur=='years')
                return toDate(new Date(this.getFullYear()-vl, this.getMonth(), this.getDate(), this.getHours(), this.getMinutes(), this.getSeconds()));
            else if(dur=='hours')
                return toDate(new Date(this.getFullYear(), this.getMonth(), this.getDate(), this.getHours()-vl, this.getMinutes(), this.getSeconds()));
            else if(dur=='minutes')
                return toDate(new Date(this.getFullYear(), this.getMonth(), this.getDate(), this.getHours(), this.getMinutes()-vl, this.getSeconds()));
            else if(dur=='seconds')
                return toDate(new Date(this.getFullYear(), this.getMonth(), this.getDate(), this.getHours(), this.getMinutes(), this.getSeconds()-vl));
            return vl;
        }
        dt.diff = function(vl, dur){
            if(!dur || dur=='days')
                return params.DATEDIF(vl, dt, 'D');
            else if(dur=='months')
                return params.DATEDIF(vl, dt, 'M');
            else if(dur=='years')
                return params.DATEDIF(vl, dt, 'Y');
            else if(dur=='hours')
                return params.DATEDIF(vl, dt, 'h');
            else if(dur=='minutes')
                return params.DATEDIF(vl, dt, 'm');
            else if(dur=='seconds')
                return params.DATEDIF(vl, dt, 's');
        }
        dt.year = function(){ return this.getFullYear(); }
        dt.month = function(){ return this.getMonth()+1; }
        dt.date = function(){ return this.getDate(); }
        dt.day = function(){ return this.getDay()+1; }
        return dt;
    }
    var asDate = function(itm, date)
    {
        if(date.add)
            return date;
        else
        {
            var vl;
            if(date instanceof Date)
                vl = toDate(date);
            else
            {
                vl = new String(date?date:'');
                var b = vl.split(/\D/);
                var dt = new Date();
                if(b.length>=3)
                {
                    b[1] = b[1]-1;
                    dt = new Date(...b);
                }
                vl = toDate(dt);
            }
            vl.getMetadata = function(){ return itm; }
            return vl;
        }
    }
    var encode = function(text) {
        if(!text) return '';
        if(typeof window !== 'undefined')
            return window.btoa(text);
        else
            return Buffer.from(text).toString('base64');
    }
    var secs =  curr.getSections();
    secs.forEach(function(sec, s){
        sec.items.forEach(function(pitem, i){
            var pval = entr[pitem.entry];
            if(pitem.entry)
            {
                if(pitem.type=='CHECKBOX')
                {
                    if(!pval) pval = [];
                    pval = Array.isArray(pval)?pval:[pval];
                    pval = pval.map(function(pv){
                        if(pv=='__other_option__')
                            return entr[pitem.entry+'-other_option_response'];
                        else
                            return pv;
                    });
                    params['entry'+pitem.entry] = asArray(pitem, pval);
                }
                else if(pitem.type=='MULTIPLE_CHOICE')
                {
                    if(pval=='__other_option__')
                        pval = entr[pitem.entry+'-other_option_response'];
                    params['entry'+pitem.entry] = asString(pitem, pval?pval:'');
                }
                else if(pitem.type=='GRID')
                {
                    var gval = [];
                    var rws = pitem.rows?pitem.rows:[];
                    rws.forEach(function(rw, rwi){
                        var val = entr[rw.entry];
                        if(rw.multiple)
                        {
                            val = val?(Array.isArray(val)?val:[val]):[];
                        }
                        else
                        {
                            val = val?val:null;
                        }
                        gval.push(val);
                    });
                    params['entry'+pitem.entry] = asArray(pitem, gval);
                }
                else if(pitem.validType=='Number' || pitem.type=='SCALE')
                {
                    if(!pval) pval = 0;
                    params['entry'+pitem.entry] = asNumber(pitem, pval);
                }
                else if(pitem.type=='DATE')
                {
                    if(!pval) pval = new Date(0);
                    params['entry'+pitem.entry] = asDate(pitem, pval);
                }
                else
                {
                    params['entry'+pitem.entry] = asString(pitem, pval?pval:'');
                }
            }
        });
    });
    var mapping = fac.mapping||{};
    for(var attr in mapping)
    {
        var iid = mapping[attr];
        var itm = items[iid]||{};
        var varname = attr.split('-').join('');
        if(attr=='score') varname = 'totalscore';
        params[varname] = params['entry'+itm.entry]||'';
    }
    params.setContext = function(ctx)
    {
        curr = ctx;
        items = ctx.data.scraped?ctx.data.scraped.items:{};
        fac = ctx.data.facade?ctx.data.facade:{};
        fcitms = fac&&fac.items?fac.items:{};
        defcurrency = fac.setting&&fac.setting.currency?fac.setting.currency:'$';
        entr = ctx.draft&&ctx.draft.entry?ctx.draft.entry:{};
    }
    params.grid = params.GRID = function(val, x, y)
    {
        var selval = val;
        if(x)
        {
            selval = val[x-1];
            if(y)
                selval = selval[y-1];
        }
        return selval;
    }
    params.num = params.NUM = function(val)
    {
        if(val)
        {
            if(isNaN(val)==false)
                return Number(val);
        }
        return 0;
    }
    params.sum = params.SUM = function()
    {
        var total = 0;
        var args = Array.prototype.slice.call(arguments);
        args.map(arg=>{
            total = total + params.NUM(arg);
        });
        return total;
    }
    params.round = params.ROUND = function(val, deci=2)
    {
        val = val + Number.EPSILON;
        var base = 10**deci;
        return Math.round(val * base) / base;
    }
    params.rounddown = params.ROUNDDOWN = num=>Math.floor(num);
    params.roundup = params.ROUNDUP = num=>Math.ceil(num);
    params.remainder = params.REMAINDER = (a,b)=>Math.round(a%b*1000000)/1000000;
    params.max = params.MAX = function(){
        var args = Array.prototype.slice.call(arguments);
        args.sort((a,b)=>b-a);
        return args[0];
    }
    params.min = params.MIN = function(){
        var args = Array.prototype.slice.call(arguments);
        args.sort((a,b)=>a-b);
        return args[0];
    }
    params.title = params.TITLE = function(val, opts={})
    {
        if(val && val.getMetadata)
        {
            var {title} = val.getMetadata()||{};
            return title||'';
        }
        return '';
    }
    params.pretty = params.PRETTY = function()
    {
        var args = Array.prototype.slice.call(arguments);
        var fargs = args.filter(function(ar){ return ar; });
        return fargs.join('<br>');
    }
    params.ifs = params.IFS = function()
    {
        var args = Array.prototype.slice.call(arguments);
        var lst;
        if(args.length%2==1)
            lst = args.pop();
        for(var i=0; i <args.length; i+=2) 
        {
            if(args[i]) return args[i+1];
        }
        if(lst==0)
            return lst;
        else
            return lst?lst:'';
    }
    params.price = params.PRICE = function(val, currency)
    {
        if(!currency) currency = defcurrency;
        if(val)
        {
            val = Array.isArray(val)?val:[val];
            if(val.length==0) return 0;
            return val.map(function(txt){
                if(!txt || !txt.split) return 0;
                var txts = txt.split(currency);
                if(txts.length>1)
                {
                    var amtstr = txts[txts.length-1];
                    amtstr = amtstr.trim();
                    if(isNaN(amtstr.charAt(0)))
                    {
                        amtstr = txts[txts.length-2];
                        var amtlastchar = amtstr.charAt(amtstr.length-1);
                        if(isNaN(amtlastchar))
                            amtstr = txts.join('');
                        else
                        {
                            var amtarr = amtstr.trim().split(/[^0-9.,]/g);
                            amtstr = amtarr[amtarr.length-1];
                        }
                    }
                    if(currency=='â‚¬' || currency=='Rp')
                        amtstr = amtstr.split(',').map(prt=>prt.split('.').join('')).join('.');
                    else if(currency=='R')
                        amtstr = amtstr.split(',').join('.').split(' ').join('');
                    else
                        amtstr = amtstr.split(',').join('');
                    amtstr = amtstr.trim().split(/[^0-9.]/g)[0];
                    if(amtstr && isNaN(amtstr)==false)
                    {
                        return Number(amtstr);
                    }
                    else
                    {
                        amtstr = txts[0];
                        amtstr = amtstr.trim().split(' ').pop();
                        if(currency=='â‚¬' || currency=='Rp')
                            amtstr = amtstr.split(',').map(prt=>prt.split('.').join('')).join('.');
                        else
                            amtstr = amtstr.split(',').join('');
                        if(amtstr && isNaN(amtstr)==false)
                            return Number(amtstr);
                    }
                }
                return 0;
            }).reduce(function(a,b){
                return a+b; 
            });
        }
        return 0;
    }
    params.quantityin = params.QUANTITYIN = ctg=>params.QUANTITY(null, ctg)
    params.quantity = params.QUANTITY = function(currency, ctg)
    {
        if(!currency) currency = defcurrency;
        var totqnt = 0;
        var lines = params.getBill(currency);
        lines.forEach(line=>{
            var [title, price, quantity, id, entry, amount, section] = line;
            var fcitm = fcitms[id]||{};
            if(fcitm.configurable=='Modifier')
            {
            }
            else if(ctg)
            {
                if(ctg==section)
                    totqnt = totqnt + Number(quantity||0);
            }
            else
                totqnt = totqnt + Number(quantity||0);
        });
        return totqnt;
    }
    params.total = params.TOTAL = params.amount = params.AMOUNT = params.amt = params.AMT = function(currency)
    {
        if(!currency) currency = defcurrency;
        var tot = 0;
        var lines = params.getBill(currency);
        lines.forEach(line=>{
            var [title, price, quantity, id, entry, amount, section] = line;
            var amt = Number(amount)||(Number(price||0)*Number(quantity||0))||0;
            tot = tot + amt;
        });
        if(citm)
        {
            citm.format = function(txtamt){
                return params.FORMAT(txtamt, setting.currency);
            }
        }
        tot = Math.round((tot + Number.EPSILON) * 100) / 100;
        return tot;
    }
    params.totalin = params.TOTALIN = function(sectitle)
    {
        var tot = 0;
        if(setting.currency)
        {
            var lines = params.getBill();
            lines.forEach(line=>{
                var [title, price, quantity, id, entry, amount, section] = line;
                var amt = Number(amount)||(Number(price||0)*Number(quantity||0))||0;
                if(sectitle && sectitle==section)
                    tot = tot + amt;
                else if(citm && citm.section && citm.section.title==section)
                    tot = tot + amt;
            });
            if(citm)
            {
                citm.format = function(txtamt){
                    return params.FORMAT(txtamt, setting.currency);
                }
            }
            tot = Math.round((tot + Number.EPSILON) * 100) / 100;
        }
        return tot;
    }
    params.html = params.HTML = val=>val?val.split('\n').join('<br>'):'';
    params.format = params.FORMAT = function(txtamt, currency)
    {
        if(!currency) currency = defcurrency;
        if(txtamt && txtamt instanceof Date)
        {
            var dt = toDate(txtamt);
            return params.formatDate(dt);
        }
        else if(isNaN(txtamt)==false)
        {
            var numamt = Number(txtamt);
            var neg = '';
            if(numamt<0)
            {
                neg = '-';
                numamt = numamt*-1;
            }
            var options = {minimumFractionDigits:2, maximumFractionDigits:2};
            if(numamt-Math.floor(numamt)==0 && numamt>=1000) options = {};
            if(currency=='\uD83D\uDCB0') options = {};
            var amtstr = Number(numamt).toLocaleString('en', options);
            if(currency.trim()=='â‚¬' || currency.trim()=='Rp')
            {
                amtstr = amtstr.split('.').map(prt=>prt.split(',').join('.')).join(',');
                if(currency.trim()=='â‚¬')
                    return neg+amtstr+currency;
                else
                    return neg+currency+amtstr;
            }
            else if(currency.trim()=='R')
            {
                amtstr = amtstr.split(',').join(' ').split('.').join(',');
                return neg+currency+amtstr;
            }
            else if(currency.trim()=='kn')
            {
                return neg+amtstr+' '+currency.trim();
            }
            else if(currency.trim()=='\uD83D\uDCB0')
            {
                return neg+amtstr+' '+currency.trim();
            }
            else if(currency.trim()=='CHF')
            {
                return neg+currency+' '+amtstr;
            }
            else
            {
                return neg+currency+amtstr;
            }
        }
        return txtamt;
    }
    params.currency = params.CURRENCY = function(currency, txtamt)
    {
        if(!currency) currency = defcurrency;
        if(citm)
        {
            citm.format = function(txtamt){
                return params.format(txtamt, currency);
            }
            return txtamt;
        }
        else
        {
            return params.format(txtamt, currency);
        }
    }
    var findText = function(val, pattern='$')
    {
        if(val)
        {
            val = Array.isArray(val)?val:[val];
            var matches = 0;
            val.map(function(txt){
                if(isNaN(txt)==true && txt.indexOf && txt.indexOf(pattern)>=0)
                {
                    matches = matches + 1;
                }
            });
            return matches>0;
        }
        return false;
    }
    var filterItems = function(pattern='$', scope=true)
    {
        var itms = [];
        secs.forEach(function(sec, s){
            sec.items.forEach(function(pitem, i){
                var pval = params['entry'+pitem.entry];
                var scoped = false;
                if(citm && citm.id==pitem.id)
                {
                    scoped = false;
                }
                else if(scope==true || scope==params.VISIBLE || scope==params.FULL)
                {
                    scoped = true;
                }
                else if(scope==params.SECTION)
                {
                    if(citm && citm.section && citm.section.id==sec.id)
                        scoped = true;
                }
                else if(scope.getMetadata)
                {
                    var ameta = scope.getMetadata();
                    if(ameta && ameta.id==pitem.id)
                        scoped = true;
                }
                var matched = false;
                if(scoped)
                {
                    var exclude = false;
                    if(pitem.logic)
                    {
                        if(pitem.logic.mode=='hide')
                        {
                            if(scope==params.FULL)
                                exclude = false;
                            else if(pitem.logic.modifierfor)
                                exclude = false;
                            else
                                exclude = true;
                        }
                        else if(pitem.logic.calculated)
                        {
                            var funcname = pitem.logic.calculated.split('(')[0];
                            funcname = funcname.toLowerCase().trim();
                            if(funcname=='${textsummary') exclude = true;
                        }
                    }
                    if(exclude==true)
                    {
                        matched = false;
                    }
                    else if(pattern==true)
                    {
                        matched = true;
                    }
                    else
                    {
                        var nval = findText(pitem.help, pattern);
                        if(nval>0)
                        {
                            matched = true;
                        }
                        else
                        {
                            nval = findText(pval, pattern);
                            if(nval>0)
                                matched = true;
                            else if(pitem.type=='GRID' && pitem.rows)
                            {
                                var prcrows = pitem.rows.filter(function(rw){
                                    return findText(rw.value, pattern)>0;
                                });
                                if(prcrows.length==pitem.rows.length)
                                    matched = true;
                            }
                        }
                    }
                }
                if(matched) itms.push(pitem);
            });
        });
        return itms;
    }
    params.textsummary = params.TEXTSUMMARY = function(pattern, all=true, wa='')
    {
        if(!pattern) pattern = '$';
        var itms = filterItems(pattern, all);
        var valitms = [];
        if(entr.emailAddress)
        {
            var ln = wa+'Email:'+wa+' '+entr.emailAddress;
            valitms.push(ln);
        }
        itms.forEach(function(item, itmi){
            var fitm = fcitms[item.id]||{};
            var enval = params['entry'+item.entry];
            var val = enval;
            if(val && val==0) val = null;
            if(val && val.length==0) val = null;
            if(item.type=='DATE' && val=='1970-01-01') val = null;
            if(item.type=='DATE' && val=='1970-01-01T01:00:00') val = null;
            if(fitm.encrypt && val) val = '******';
            if(citm && citm.id==item.id){}
            else if(item.type=='GRID' && val)
            {
                var valids = val?val.filter(function(vl){ return vl&&vl.length>0; }):[];
                if(valids.length>0)
                {
                    if(item.title)
                        valitms.push(item.title);
                    item.rows.forEach(function(rw, r){
                        var rvals = val[r];
                        if(rvals && rvals.length==0) rvals = null;
                        if(rvals)
                        {
                            rvals = Array.isArray(rvals)?rvals:[rvals];
                            var ln = wa+rw.value+':'+wa+' '+rvals.join(', ');
                            valitms.push(ln);
                        }
                    });
                }
            }
            else if(item.type=='PARAGRAPH_TEXT' && fitm.measure=='Configurable' && val)
            {
                if(item.title) valitms.push(wa+item.title+wa);
                var cis = val.split('\n-----\n')||[];
                cis.forEach((cistr, c)=>{
                    var rws = cistr.split('\n');
                    rws.forEach(rw=>{
                        var splt = rw.trim().split(' | ');
                        var ln = splt.pop();
                        var [vid, qnt] = ln.trim().split(' * ');
                        var ttl = splt.join(' | ');
                        if(fitm.modifierfor) ttl = 'â†³ '+ttl;
                        if(vid && qnt) valitms.push(wa+ttl+':'+wa+' '+qnt);
                    });
                });
            }
            else if(!item.product && item.type=='LIST' && enval=='0')
            {
                var ln = wa+item.title+':'+wa+' '+enval;
                valitms.push(ln);
            }
            else if(val && val instanceof Date)
            {
                var ln = wa+item.title+':'+wa+' '+params.formatDate(val);
                valitms.push(ln);
            }
            else if(val)
            {
                var ln = wa+item.title+':'+wa+' ';
                if(fitm.measure=='Weight')
                {
                    var unit = setting.currencyCode=='USD'?'lbs':'kg';
                    ln = wa+item.title+' ('+unit+'):'+wa+' ';
                }
                if(item.format)
                    ln += item.format(val);
                else
                    ln += Array.isArray(val)?val.join(', '):val;
                valitms.push(ln);
            }
        });
        return valitms.join('\r\n');
    }
    params.getSummaryRows = function(pattern, all=true)
    {
        if(!pattern) pattern = '$';
        var itms = filterItems(pattern, all);
        var valitms = [];
        if(entr.emailAddress)
        {
            var ln = '<tr><td>Email:</td><td>'+entr.emailAddress+'</td></tr>';
            valitms.push(ln);
        }
        itms.forEach(function(item, itmi){
            var fitm = fcitms[item.id]||{};
            var val = params['entry'+item.entry];
            if(val && val.length==0) val = null;
            if(val && val==0) val = null;
            if(item.type=='DATE' && val=='1970-01-01') val = null;
            if(item.type=='DATE' && val=='1970-01-01T01:00:00') val = null;
            if(fitm.encrypt && val) val = '******';
            if(item.type=='GRID' && val)
            {
                var valids = val?val.filter(function(vl){ return vl&&vl.length>0; }):[];
                if(valids.length>0)
                {
                    if(item.title)
                        valitms.push('<tr><td colspan="2">'+item.title+'</td></tr>');
                    item.rows.forEach(function(rw, r){
                        var rvals = val[r]
                        if(rvals && rvals.length==0) rvals = null;
                        if(rvals)
                        {
                            rvals = Array.isArray(rvals)?rvals:[rvals];
                            valitms.push('<tr><td>'+rw.value+':</td><td>'+rvals.join(', ')+'</td></tr>');
                        }
                    });
                }
            }
            else if(item.type=='PARAGRAPH_TEXT' && val)
            {
                if(fitm.measure=='Configurable')
                {
                    valitms.push('<tr><td colspan="2">'+item.title+'</td></tr>');
                    var cis = val.split('\n-----\n')||[];
                    cis.forEach(cistr=>{
                        var rws = cistr.split('\n');
                        rws.forEach(rw=>{
                            var splt = rw.trim().split(' | ');
                            var ln = splt.pop();
                            var [vid, qnt] = ln.trim().split(' * ');
                            var ttl = splt.join(' | ');
                            if(fitm.modifierfor) ttl = 'â†³ '+ttl;
                            if(vid && qnt) valitms.push('<tr><td>'+ttl+':</td><td>'+qnt+'</td></tr>');
                        });
                    });
                }
                else
                {
                    var ln = '<tr><td>'+item.title+':</td>';
                    ln += '<td>'+val.split('\n').join('<br>')+'</td></tr>';
                    valitms.push(ln);
                }
            }
            else if(val && val instanceof Date)
            {
                var ln = '<tr><td>'+item.title+':</td>';
                ln += '<td>'+params.formatDate(val)+'</td></tr>';
                valitms.push(ln);
            }
            else if(val && fitm.subtype == 'SIGNATURE')
            {
                // Don't display signature in response summary & in the email summary
                // var ln = '<tr><td>'+item.title+':</td>';
                // ln += '<td><img src="'+val+'"/></td></tr>';
                // valitms.push(ln);
            }
            else if(val)
            {
                var ln = '<tr><td>'+item.title+':</td> ';
                if(fitm.measure=='Weight')
                {
                    var unit = setting.currencyCode=='USD'?'lbs':'kg';
                    ln = '<tr><td>'+item.title+' ('+unit+'):</td> ';
                }
                if(item.format)
                    ln += '<td>'+item.format(val)+'</td></tr>';
                else
                    ln += '<td>'+(Array.isArray(val)?val.join(', '):val)+'</td></tr>';
                valitms.push(ln);
            }
        });
        return valitms;
    }
    params.summary = params.SUMMARY = function(pattern, all=true)
    {
        var valitms = params.getSummaryRows(pattern, all);
        var tbl = '<table class="ff-summary ff-email">'
        +'<colgroup><col class="ff-col-name"/><col class="ff-col-value"/></colgroup>'
        +valitms.join('\n')+'</table>';
        if(curr.isEditMode && curr.isEditMode())
            tbl += '<p class="pt-1 pb-1"><a class="card-link" href="javascript:void(0)" onclick="editFacade.showMapping()">Configure</a></p>';
        return tbl;
    }
    params.healthsummary = params.HEALTHSUMMARY = function()
    {
        var valitms = params.getSummaryRows(true, true);
        ['customer-id', 'score', 'diagnosis'].forEach(attr=>{
            var map = fac.mapping||{};
            var iid = map[attr];
            var item = items[iid]||{};
            var fcitm = fcitms[iid]||{};
            var val = params['entry'+item.entry]||'';
            if(val && fcitm.mode=='hide')
            {
                valitms.push('<tr><td>'+item.title+':</td> '+
                '<td>'+(Array.isArray(val)?val.join(', '):val)+'</td></tr>');
            }
        });
        var tbl = '<table class="ff-summary ff-email">'+valitms.join('\n')+'</table>';
        tbl += '<colgroup><col class="ff-col-name"/><col class="ff-col-value"/></colgroup>';
        if(curr.isEditMode && curr.isEditMode())
            tbl += '<p class="pt-1 pb-1"><a class="card-link" href="javascript:void(0)" onclick="editFacade.showMapping()">Configure</a></p>';
        return tbl;
    }
    params.computeCondition = function(secid)
    {
        var itmsubmits = fac.submit||{};
        var itmsubmit = itmsubmits[secid]||{};
        var cond = itmsubmit.ifmsg;
        if(cond && cond.source)
        {
            var srcitm = items[cond.source];
            if(!srcitm) return;
            var srcval = entr[srcitm.entry];
            if(srcval)
                srcval = isNaN(srcval)?0:Number(srcval);
            else
                srcval = 0;
            var ifs = cond.ifs?cond.ifs:[];
            for(var i=0; i<ifs.length; i++)
            {
                var ifitm = ifs[i];
                var estr = srcval;
                if(!ifitm.val) ifitm.val = 0;
                if(!ifitm.altval) ifitm.altval = 0;
                if(ifitm.op=='><')
                    estr += ' >= '+ifitm.val+' && '+srcval+' <= '+ifitm.altval;
                else
                    estr += ' '+ifitm.op+' '+ifitm.val;
                var rslt = eval(estr);
                if(rslt)
                {
                    var ifrslt = curr.computeField(ifitm.re);
                    return ifrslt;
                }
            }
            if(cond.els)
            {
                var elsrslt = curr.computeField(cond.els.re);
                return elsrslt;
            }
        }
        return;
    }
    params.scoresummary = params.SCORESUMMARY = function(secid)
    {
        var itmsubmits = fac.submit||{};
        var secs =  curr.getSections();
        var secids = secs.filter(sec=>{
            var itmsubmit = itmsubmits[sec.id];
            return itmsubmit&&itmsubmit.submitto=='ifmsg'&&itmsubmit.ifmsg;
        }).map(sec=>sec.id);
        if(secid) secids = [secid];
        if(secids.length>0)
            return params.computeCondition(secids.at(-1))||'(No message)';
        else
            return '(No message)';
    }
    params.formatDate = function(val)
    {
        var lang = setting.locale||setting.language;
        var vlmeta = val.getMetadata?val.getMetadata():null;
        if(vlmeta && vlmeta.time==1 && val.toLocaleString)
            return lang?val.toLocaleString(lang):val.toLocaleString();
        else if(val.toLocaleDateString)
            return lang?val.toLocaleDateString(lang):val.toLocaleDateString();
        else
            return val;
    }
    params.getBillFooter = function()
    {
        var mp = fac.mapping||{};
        return [
            'amount', 'service', 'taxes', 'delivery-fee',
            'tip', 'donation', 'discount', 'net-amount'
        ].map(attr=>{
            var iid = mp[attr];
            var itm = items[iid];
            if(itm)
            {
                itm.mapped = attr;
                itm.format = txtamt=>params.format(txtamt, defcurrency);
                var val = params['entry'+itm.entry];
                return attr=='discount'?asNumber(itm, val*-1):val;
            }
        }).filter(val=>{
            if(val==0) return false;
            return val;
        });
    }
    params.bill = params.BILL = function()
    {
        var currency = defcurrency;
        var args = [].slice.call(arguments);
        if(args.length>0)
            currency = args.shift();
        else
            args = params.getBillFooter();
        return params.toBill(currency, args);
    }
    params.orders = params.ORDERS = function()
    {
        var currency = defcurrency;
        var args = params.getBillFooter();
        return params.toBill(currency, args);
    }
    params.toBill = function(currency, args)
    {
        var lines = params.getBill(currency);
        var tbl = curr.lang('- Your cart is empty -');
        if(lines.length>0)
        {
            var header = [curr.lang('Item'), curr.lang('Unit price'), curr.lang('Qty'), curr.lang('Amount')];
            var thead = '<tr><td>'+header.join('</td><td>')+'</td></tr>';
            var estamt = 0;
            var rows = lines.map(function(oline, l){
                var [ttl, prc, qty, iid, entry, discamt, sec] = oline;
                if(!qty) qty = 0;
                ttl = ttl.split('\n').join('<br/>');
                var line = [ttl, prc, qty];
                var lamt = prc*qty;
                estamt += lamt;
                if(discamt && discamt<lamt)
                    line.push(params.format(discamt, currency)+'<s>'+params.format(lamt, currency)+'</s>');
                else if(discamt && discamt>lamt)
                    line.push(params.format(discamt, currency));
                else
                    line.push(params.format(lamt, currency));
                line[1] = params.format(prc, currency);
                return '<tr><td>'+line.join('</td><td>')+'</td></tr>';
            }).join('\n');
            var tfoot = args.map(function(foot, l){
                var ttl = '';
                if(foot.getMetadata && foot.getMetadata())
                {
                    var meta = foot.getMetadata();
                    if(meta.title) ttl = meta.title;
                    if(meta.mapped=='amount' && estamt>foot)
                    {
                        var saved = estamt - foot;
                        saved = meta.format?meta.format(saved):saved;
                        var langed = curr.lang('You saved $amount', {amount:saved});
                        ttl += '<br/><span class="ff-bill-saved">('+langed+')</span>';
                    }
                    foot = meta.format?meta.format(foot):foot;
                }
                return '<tr><td colspan="3">'+ttl+'</td><td>'+foot+'</td></tr>';
            }).join('\n');
            var tbl = '<table class="ff-bill ff-email"><colgroup><col/><col/><col/><col/></colgroup><thead>'+thead+'</thead><tbody>'+rows+'</tbody><tfoot>'+tfoot+'</tfoot></table>';
            tbl = params.inlineCSS(tbl);
        }
        if(curr.isEditMode && curr.isEditMode())
            tbl += '<p class="pt-1 pb-1"><a class="card-link" href="javascript:void(0)" onclick="editFacade.showMapping()">Configure</a></p>';
        return tbl;
    }
    params.textbill = params.TEXTBILL = function()
    {
        var currency = defcurrency;
        var args = [].slice.call(arguments);
        if(args.length>0)
            currency = args.shift();
        else
            args = params.getBillFooter();
        var lines = params.getBill(currency);
        if(lines.length==0) return '';
        var rows = lines.map(function(line){
            var [ttl, prc, qty, iid, entry, discamt, sec] = line;
            var lamt = discamt?discamt:prc*qty;
            lamt = params.format(lamt, currency);
            prc = params.format(prc, currency);
            return ttl+': '+prc+' * '+qty+' = '+lamt;
        });
        var foots = args.map(function(foot){
            var ttl = '';
            if(foot.getMetadata)
            {
                var meta = foot.getMetadata();
                if(meta && meta.title) ttl = meta.title+': ';
                foot = meta&&meta.format?meta.format(foot):foot;
            }
            return ttl+foot;
        });
        return rows.join('\n')+'\n'+foots.join('\n');
    }
    params.getBill = function(currency)
    {
        if(curr.cachedBill) return curr.cachedBill;
        if(!currency) currency = defcurrency;
        var combos = {};
        var lines = new Array();
        var secs =  curr.getSections();
        secs.forEach(function(sec, s){
            sec.items.forEach(function(item, i){
                var fitm = fcitms[item.id]||{};
                var amt = params.price(item.help, currency);
                var fullprc = fitm.fullprice||amt;
                fullprc = isNaN(fullprc)?0:Number(fullprc);
                if(item.id==mapping.service)
                {
                    var srvstr = entr[item.entry];
                    var srv = srvstr&&isNaN(srvstr)==false?Number(srvstr):0;
                    if(srv) lines.push([item.title||'', srv, 1, item.id, item.entry, srv, sec.title]);
                }
                else if(item.type=='LIST' || (item.type=='TEXT'&&fitm.widget=='product') || item.type=='MULTIPLE_CHOICE' || item.type=='SCALE')
                {
                    if(fitm.choices && fitm.discounted)
                    {
                        var selstr = entr[item.entry];
                        fitm.choices.forEach((ch,c)=>{
                            if(ch==selstr)
                            {
                                var disc = fitm.discounted[c];
                                if(!disc) disc = amt*ch;
                                lines.push([item.title||'', fullprc, ch, item.id, item.entry, disc, sec.title]);
                            }
                        });
                    }
                    else if(amt>0 || fitm.widget=='product')
                    {
                        var qntstr = entr[item.entry];
                        var qnt = qntstr?(isNaN(qntstr)==false?Number(qntstr):1):0;
                        if(qnt) lines.push([qnt>0||qntstr.toString()=='1'?(item.title||''):(item.title+' | '+qntstr), fullprc, qnt, item.id, item.entry, amt*qnt, sec.title]);
                    }
                    else
                    {
                        var amtstr = entr[item.entry];
                        amt = params.price(amtstr, currency);
                        fullprc = fullprc||amt;
                        if(amt>0) lines.push([item.title+' | '+amtstr, fullprc, 1, item.id, item.entry, amt, sec.title]);
                    }
                }
                else if(item.type=='TEXT')
                {
                    var qntstr = entr[item.entry];
                    var qnt = qntstr&&isNaN(qntstr)==false?Number(qntstr):0;
                    if(amt && qnt) lines.push([item.title||'', fullprc, qnt, item.id, item.entry, amt*qnt, sec.title]);
                }
                else if(item.type=='PARAGRAPH_TEXT')
                {
                    var cfgstr = entr[item.entry];
                    if((fitm.measure=='Configurable' || fitm.measure === 'Nested') && cfgstr)
                    {
                        var cis = cfgstr?cfgstr.split('\n-----\n'):[];
                        cis.forEach((cistr, c)=>{
                            var rws = cistr.split('\n');
                            rws.forEach(rw=>{
                                var ttl = item.title;
                                if(fitm.modifierfor)
                                    ttl = 'â†³ '+ttl;
                                var prc = fitm.price;
                                if(fitm.measure === 'Nested')
                                {
                                    if(rw.indexOf(' || ') == -1)
                                    {
                                        return;
                                    }
                                    var [v, v1Qnt] = rw.trim().split(' || ');
                                    var vid = v.trim().split(' | ').pop();
                                    var [v1, qnt] = v1Qnt.trim().split(' * ');
                                    var v1id = v1.trim().split(' | ').pop();
                                    var vrn = fitm.variants[vid];
                                    var v1rn = fitm.variants1[v1id];
                                    if(vrn&&vrn.name) ttl = ttl+' | '+vrn.name;
                                    if(v1rn&&v1rn.name) ttl = ttl+' | '+v1rn.name;
                                    var variantConfig = fitm.variantConfig || {};
                                    var inventory = variantConfig.inventory || {};
                                    var inv = inventory[vid + '-' + v1id] || {};
                                    if(inv.price)
                                    {
                                        prc = inv.price;
                                    }
                                    var modline = [ttl, prc, qnt, item.id, item.entry, 0, sec.title, vid, v1id];
                                    lines.push(modline);
                                    return;
                                }
                                var ln = rw.trim().split(' | ').pop();
                                var [vid, qnt] = ln.trim().split(' * ');
                                var vrn = fitm.variants?fitm.variants[vid]:null;
                                if(vrn&&vrn.name) ttl = ttl+' | '+vrn.name;
                                if(vrn&&vrn.price) prc = vrn.price;
                                var modline = [ttl, prc, qnt, item.id, item.entry, 0, sec.title, vid];
                                if(fitm.modifierfor)
                                {
                                    var modifiers = combos[fitm.modifierfor]||{};
                                    var modlines = modifiers[item.id]||[];
                                    modline.push(fitm.modifierfor);
                                    modline.push(c);
                                    modlines.push(modline);
                                    modifiers[item.id] = modlines;
                                    combos[fitm.modifierfor] = modifiers;
                                }
                                else
                                {
                                    lines.push(modline);
                                }
                            });
                        });
                    }
                }
                else if(item.type=='CHECKBOX')
                {
                    if(amt>0)
                    {
                        var qntstrs = entr[item.entry];
                        qntstrs = Array.isArray(qntstrs)?qntstrs:[qntstrs];
                        qntstrs.forEach(function(qntstr){
                            var qnt = qntstr?(isNaN(qntstr)==false?Number(qntstr):1):0;
                            if(amt && qnt) lines.push([qnt>1||qntstr.toString()=='1'?item.title:(item.title+' | '+qntstr), fullprc, qnt, item.id, item.entry, 0, sec.title]);
                        });
                    }
                    else
                    {
                        var vals = entr[item.entry];
                        var vals = Array.isArray(vals)?vals:[vals];
                        vals.forEach(function(val){
                            var vlamt = params.price(val, currency);
                            if(vlamt>0) lines.push([item.title+' | '+val, vlamt, 1, item.id, item.entry, 0, sec.title]);
                        });
                    }
                }
                else if(item.type=='GRID')
                {
                    var rws = item.rows?item.rows:[];
                    rws.forEach(function(rw, rwi){
                        var valmap = {};
                        var val = entr[rw.entry];
                        val = Array.isArray(val)?val:[val];
                        val.forEach(function(vl){ valmap[vl] = vl; });
                        item.choices.forEach(function(ch, chi){
                            if(ch && ch.value && valmap[ch.value])
                            {
                                var ttls = [];
                                if(item.title)
                                    ttls.push(item.title);
                                if(isNaN(rw.value))
                                    ttls.push(rw.value);
                                if(isNaN(ch.value))
                                    ttls.push(ch.value);
                                var ttl = ttls.join(' | ');
                                if(amt>0)
                                {
                                    var qnt = ch.value?(isNaN(ch.value)==false?Number(ch.value):1):0;
                                    if(amt && qnt) lines.push([ttl, amt, qnt, item.id, item.entry, 0, sec.title]);
                                }
                                else
                                {
                                    var rwamt = params.price(rw.value, currency);
                                    if(rwamt>0)
                                    {
                                        var qnt = ch.value?(isNaN(ch.value)==false?Number(ch.value):1):0;
                                        if(rwamt && qnt) lines.push([ttl, rwamt, qnt, item.id, item.entry, 0, sec.title]);
                                    }
                                } 
                            }
                        });
                    });
                }
            });
        });
        var merged = [];
        lines.forEach(line=>{
            merged.push(line);
            var [ttl, prc, qty, iid] = line;
            qty = isNaN(qty)?0:Number(qty);
            var combo = combos[iid];
            if(combo)
            {
                var comboitm = fcitms[iid]||{};
                var modifiers = comboitm.modifiers||{};
                var modlist = Object.entries(modifiers).map(entry=>{
                    var [modifierId, value] = entry;
                    return Object.assign({modifierId}, value);
                });
                modlist.sort((a,b)=>a.index-b.index);
                modlist = modlist.map(o=>o.modifierId);
                for(var i=0; i<qty; i++)
                {
                    modlist.forEach(modifierId=>{
                        var modlines = combo[modifierId]||[];
                        modlines.forEach(modline=>{
                            var m = modline.at(-1);
                            if(m==i) merged.push(modline);
                        });
                    });
                }
            }
        });
        merged.toString = function(){ return JSON.stringify(merged); }
        curr.cachedBill = merged;
        return merged;
    }
    params.billto = params.BILLTO = function()
    {
        var mp = fac.mapping||{};
        var tbl = ['name', 'address', 'delivery-zone', 'email', 'phone'].map(attr=>{
            var iid = mp[attr];
            if(iid)
            {
                var itm = items[iid];
                if(itm) return params['entry'+itm.entry];
            }
        }).filter(val=>val&&val.toString()).map(val=>val.trim().split('\n').join('<br/>')).join('<br/>');
        if(curr.isEditMode && curr.isEditMode())
            tbl += `<p class="pt-1 pb-1">
                        <a class="card-link" href="javascript:void(0)" 
                            onclick="editFacade.showMapping('pills-Customer-tab')">
                            Configure
                        </a>
                    </p>`;
        return tbl;
    }
    params.menu = params.MENU = function(currency, secttl=true)
    {
        if(!currency) currency = defcurrency;
        var lines = '';
        var secs =  curr.getSections();
        secs.forEach(function(sec, s){
            var prds = [];
            sec.items.forEach(function(item, i){
                var fcitm = fcitms[item.id]||{};
                var amt = params.price(item.help, currency);
                if(fcitm.widget=='product' || amt)
                {
                    var prd = '<div class="ff-menu-prd">'+item.title+'</div>';
                    var prc = '<div class="ff-menu-prc">'+params.format(amt, currency)+'</div>';
                    var row = '<div class="ff-menu" onclick="formFacade.directtoSection(\''+sec.id+'\', \''+item.id+'\')">'+prd+prc+'</div>';
                    prds.push(row);
                }
            });
            if(prds.length>0)
            {
                lines += '<div class="ff-menu-sec">\n';
                if(secttl)
                    lines += '<div class="ff-menu-ttl" onclick="formFacade.directtoSection(\''+sec.id+'\')">'+sec.title+'</div>\n';
                lines += prds.join('\n')+'</div>';
            }
        });
        return lines;
    }
    params.categories = params.CATEGORIES = function()
    {
        var lines = [];
        var next = fac&&fac.next?fac.next:{};
        var publishId = curr.data.request.params.publishId;
        var secs =  curr.getSections();
        secs.forEach(function(sec, s){
            var prds = sec.items.filter(itm=>{
                if(!curr.getPrice) return false;
                var prc = curr.getPrice(itm, defcurrency);
                var visible = true;
                var fitm = fcitms[itm.id]||{};
                if(fitm.mode=='hide') visible = false;
                if(fitm.inventory=='yes'&&fitm.remain<=0) visible = false;
                return (prc.min>0||prc.max>0) && visible;
            });
            var nav = next[sec.id];
            if(nav && nav.navigation=='added' && prds.length>0)
            {
                var img = 'https://neartail.com/img/collections.svg';
                var imgs = sec.items.filter(itm => itm.type=='IMAGE' && (itm.image || itm.blob));
                var imgttls = sec.items.filter(itm => itm.titleImage && (itm.titleImage.image || itm.titleImage.blob));
                var fcimgs = sec.items.map(itm=>fcitms[itm.id]).filter(fcitm=>fcitm&&fcitm.prdimage);
                if(imgs.length>0){
                    if(imgs[0].image)
                        img = 'https://formfacade.com/itemload/item/'+encode(imgs[0].image);
                    else
                        img = 'https://formfacade.com/itemembed/'+publishId+'/item/'+imgs[0].id+'/image/'+imgs[0].blob;
                } else if(fcimgs.length>0) {
                    img = fcimgs[0].prdimage;
                } else if(imgttls.length>0) {
                    if(imgttls[0].image)
                        img = 'https://formfacade.com/itemload/item/'+encode(imgttls[0].image);
                    else
                        img = 'https://formfacade.com/itemimg/'+publishId+'/item/'+imgttls[0].id+'/title/'+imgttls[0].titleImage.blob;
                }
                var ivk = curr.isEditMode&&curr.isEditMode()?"formFacade.scrollIntoView(document.getElementById('ff-sec-"+sec.id+"'))":"formFacade.directtoSection('"+sec.id+"')";
                if(window.isFormBuilder) {
                    ivk = "";
                }
                var line = '<li class="ff-image-list__item" onclick="'+ivk+'">';
                if(img)
                    line = line + '<img class="ff-image-list__image" src="'+img.replace('neartail.com/', 'formfacade.com/')+'">';
                line = line + '<div class="ff-image-list__supporting"><span class="ff-image-list__label">'+sec.title+'</span></div></li>'
                lines.push(line);
            }
        });
        if(curr.isEditMode && curr.isEditMode())
        {
            var line = '<li class="ff-image-list__item" onclick="editFacade.showNavigation(); formFacade.closeNavigation();">';
            line = line + '<img class="ff-image-list__image" src="/img/edit_pencil.svg" title="Show or hide product categories in navigation">';
            line = line + '<div class="ff-image-list__supporting"><span class="ff-image-list__label">&nbsp;</span></div></li>'
            lines.push(line);
        }
        return '<ul class="ff-image-list nt-image-list ff-image-list__supporting">'+lines.join('\n')+'</ul>';
    }
    params.inlineCSS = table=>curr.juice?curr.juice(`
        <style>
            .ff-email{ min-width:400px; max-width:100%; table-layout:fixed; border-collapse:collapse; border:1px solid #eee; }
            .ff-email tr{ height:36px; }
            .ff-email tr:nth-child(odd){ background-color:#f5f5f5; }
            .ff-email td{ padding:6px; }
            .ff-summary col{ width:50%; }
            .ff-bill col{ width:25%; }
            .ff-bill thead tr{ font-weight:600; background-color:#111; color:#fff; }
        </style>
        ${table}
    `):table;
    params.response = params.RESPONSE = function(scope)
    {
        var table = fac.hipaache?params.healthsummary():params.SUMMARY(true, scope||true);
        table = params.inlineCSS(table);
        return table;
    }
    params.getFields = function(args)
    {
        var itms = [];
        args = Array.prototype.slice.call(args);
        if(args.length>1)
        {
            var [scope, ctgnm] = args;
            if(scope==params.CATEGORY && ctgnm)
            {
                curr.getSections().forEach(function(sec, s){
                    sec.items.forEach(function(itm, i){
                        var fcitm = fcitms[itm.id]||{};
                        if(fcitm.tag==ctgnm) itms = itms.concat(itm);
                    });
                });
            }
            else
                itms = args.filter(arg=>arg.getMetadata).map(arg=>arg.getMetadata());
        }
        else
        {
            var scope = args[0];
            if(!scope) scope = params.ALL;
            if(scope.getMetadata)
                itms = [scope.getMetadata()];
            else
            {
                var secs =  curr.getSections();
                var secid = citm&&citm.section?citm.section.id:null;
                secs.forEach(function(sec, s){
                    if(scope==params.SECTION && secid==sec.id)
                        itms = itms.concat(sec.items);
                    else if(scope==params.ALL)
                        itms = itms.concat(sec.items);
                });
            }
        }
        return itms.filter(itm=>itm.type=='GRID'||itm.entry);
    }
    params.getScorable = function(args)
    {
        return params.getFields(args).filter(item=>{
            var fitm = fcitms[item.id];
            return fitm && fitm.score;
        });
    }
    params.viewscore = params.VIEWSCORE = function()
    {
        return ejs.render(curr.template.viewscore, curr);
    }
    params.points = params.POINTS = function()
    {
        var acc = 0;
        var itms = params.getScorable(arguments);
        itms.forEach(function(item, itmi){
            var fitm = fcitms[item.id]||{};
            if(item.type=='GRID')
            {
                var rws = item.rows?item.rows:[];
                rws.forEach(function(rw, rwi){
                    var valmap = {};
                    var val = entr[rw.entry];
                    val = Array.isArray(val)?val:[val];
                    val.forEach(function(vl){ valmap[vl] = vl; });
                    item.choices.forEach(function(ch, chi){
                        if(ch && valmap[ch.value])
                        {
                            var point = fitm.score[chi];
                            if(point) acc = acc + point;
                        }
                    });
                });
            }
            else if(item.choices)
            {
                var itmacc = 0;
                var valmap = {};
                var val = entr[item.entry];
                val = Array.isArray(val)?val:[val];
                val.forEach(function(vl){ valmap[vl] = vl; });
                item.choices.forEach(function(ch, chi){
                    if(ch && valmap[ch.value])
                    {
                        var point = fitm.score[chi];
                        if(point) itmacc = itmacc + point;
                    }
                });
                if(fitm.maxscore>0)
                    acc = acc + (itmacc>fitm.maxscore?Number(fitm.maxscore):itmacc);
                else
                    acc = acc + itmacc;
            }
        });
        return acc;
    }
    params.answered = params.ANSWERED = function()
    {
        var acc = 0;
        var itms = params.getScorable(arguments);
        itms.forEach(function(item){
            var fitm = fcitms[item.id]||{};
            var fscore = fitm.score||[];
            var val = entr[item.entry];
            if(item.type=='GRID')
            {
                acc = acc + item.rows.filter(rw=>{
                    var rval = entr[rw.entry];
                    var rarr = Array.isArray(rval)?rval:[rval];
                    return item.choices.filter(function(ch, chi){
                        if(rarr.indexOf(ch.value)>=0)
                            return fscore[chi]>0||fscore[chi]<0||fscore[chi]==0;
                    }).length>0;
                }).length;
            }
            else if(item.choices && val)
            {
                var valarr = Array.isArray(val)?val:[val];
                var validscore = item.choices.filter(function(ch, chi){
                    if(valarr.indexOf(ch.value)>=0)
                        return fscore[chi]>0||fscore[chi]<0||fscore[chi]==0;
                });
                if(validscore.length>0) acc = acc + 1;
            }
        });
        return acc;
    }
    params.averagepoints = params.AVERAGEPOINTS = function()
    {
        var pns = params.POINTS.apply(params, arguments);
        var ans = params.ANSWERED.apply(params, arguments);
        if(pns==0 || ans==0) return 0;
        return Math.round((pns/ans + Number.EPSILON) * 100) / 100;
    }
    params.score = params.SCORE = function()
    {
        var args = Array.prototype.slice.call(arguments);
        var scope = args.shift();
        if(!scope) scope = true;
        var itms;
        if(scope.getMetadata)
            itms = [scope.getMetadata()];
        else if(scope==true || scope==params.ALL || scope==params.SECTION)
            itms = filterItems(true, scope);
        else
            itms = filterItems(scope, params.ALL);
        var acc = 0;
        itms.forEach(function(item, itmi){
            if(item.type=='MULTIPLE_CHOICE' || item.type=='CHECKBOX' || (scope.getMetadata && item.type=='LIST'))
            {
                var valmap = {};
                var val = entr[item.entry];
                val = Array.isArray(val)?val:[val];
                val.forEach(function(vl){ valmap[vl] = vl; });
                item.choices.forEach(function(ch, chi){
                    if(ch && valmap[ch.value])
                    {
                        var point = args[chi];
                        if(point) acc = acc + point;
                    }
                });
            }
            else if(item.type=='GRID')
            {
                var rws = item.rows?item.rows:[];
                rws.forEach(function(rw, rwi){
                    var valmap = {};
                    var val = entr[rw.entry];
                    val = Array.isArray(val)?val:[val];
                    val.forEach(function(vl){ valmap[vl] = vl; });
                    item.choices.forEach(function(ch, chi){
                        if(ch && valmap[ch.value])
                        {
                            var point = args[chi];
                            if(point) acc = acc + point;
                        }
                    });
                });
            }
        });
        return acc;
    }
    params.diff = params.DIFF = function(val=0)
    {
        if(!val.getMetadata) return;
        var num = isNaN(val)?0:Number(val);
        var {entry} = val.getMetadata()||{};
        var history = draft.history||{};
        var prev = history.entry||{};
        var preval = prev[entry]||0;
        var prenum = isNaN(preval)?0:Number(preval);
        return num - prenum;
    }
    params.mostly = params.MOSTLY = function()
    {
        var args = Array.prototype.slice.call(arguments);
        var scope = args[0];
        if(!scope) scope = true;
        var itms;
        if(scope.getMetadata)
            itms = [scope.getMetadata()];
        else if(scope==true || scope==params.ALL || scope==params.SECTION)
            itms = filterItems(true, scope);
        else
            itms = filterItems(scope, params.ALL);
        var occs = {};
        itms.forEach(function(item, itmi){
            if(item.type=='MULTIPLE_CHOICE' || item.type=='CHECKBOX')
            {
                var valmap = {};
                var val = entr[item.entry];
                val = Array.isArray(val)?val:[val];
                val.forEach(function(vl){ valmap[vl] = vl; });
                item.choices.forEach(function(ch, chi){
                    if(ch && valmap[ch.value])
                    {
                        var occ = occs[chi];
                        occs[chi] = occ?(occ+1):1;
                    }
                });
            }
            else if(item.type=='GRID')
            {
                var rws = item.rows?item.rows:[];
                rws.forEach(function(rw, rwi){
                    var valmap = {};
                    var val = entr[rw.entry];
                    val = Array.isArray(val)?val:[val];
                    val.forEach(function(vl){ valmap[vl] = vl; });
                    item.choices.forEach(function(ch, chi){
                        if(ch && valmap[ch.value])
                        {
                            var occ = occs[chi];
                            occs[chi] = occ?(occ+1):1;
                        }
                    });
                });
            }
        });
        var occlst = [];
        for(var chi in occs)
            occlst.push({index:chi, value:occs[chi]});
        occlst.sort((a,b)=>b.value-a.value);
        var rank = args[1]?args[1]:1;
        if(occlst.length>=rank)
            return parseInt(occlst[rank-1].index)+1;
        else
            return 0;
    }
    params.weight = params.WEIGHT = function()
    {
        var weightfn = 0;
        var lines = params.getBill(defcurrency);
        lines.forEach(function(oline){
            var [ttl, prc, qty, iid, entry, discamt, sec, vid, v1id] = oline;
            var fcitm = fcitms[iid]||{};
            if(fcitm.shipped)
            {
                if(fcitm.measure === 'Nested' && fcitm.variantConfig && fcitm.variantConfig.inventory)
                {
                    if(vid && v1id)
                    {
                        var shippingWeightStr = fcitm.variantConfig.inventory[vid + '-' + v1id].ship || 0;
                        var shippingWeight = Number(shippingWeightStr);
                        if(isNaN(shippingWeight)) shippingWeight = 0;
                        var weightrw = Number(shippingWeight) * qty;
                        weightfn += weightrw;
                    }
                }
                else if(fcitm.variants)
                {
                    var vrn = fcitm.variants[vid]||{};
                    var weightrw = Number(vrn.ship||0)*qty;
                    weightfn = weightfn + weightrw;
                }
                else if(fcitm.ship)
                {
                    var weightrw = Number(fcitm.ship)*qty;
                    weightfn = weightfn + weightrw;
                }
            }
        });
        return weightfn;
    }
    params.tax = params.TAX = function()
    {
        var taxfn = 0;
        var lines = params.getBill(defcurrency);
        lines.forEach(function(oline){
            var [ttl, prc, qty, iid, entry, discamt, sec] = oline;
            var fcitm = fcitms[iid]||{};
            var taxpc = Number(fcitm.tax||0);
            var amtrw = discamt||prc*qty;
            var taxrw = amtrw*(taxpc/100);
            taxfn = taxfn + taxrw;
        });
        return taxfn;
    }
    params.fee = params.FEE = function()
    {
        var args = Array.prototype.slice.call(arguments);
        var currency = args.shift();
        if(citm)
        {
            citm.format = function(txtamt){
                return params.format(txtamt, currency);
            }
        }
        return params.score.apply(params, args);
    }
    params.charge = params.CHARGE = function()
    {
        var args = Array.prototype.slice.call(arguments);
        args.unshift(defcurrency);
        return params.fee.apply(params, args);
    }
    params.nettotal = params.NETTOTAL = function()
    {
        var tot = params.total(defcurrency);
        var mp = fac.mapping?fac.mapping:{};
        ['service', 'taxes', 'delivery-fee', 'tip', 'donation', 'discount'].forEach(attr=>{
            var iid = mp[attr];
            if(iid)
            {
                var itm = items[iid];
                if(itm)
                {
                    var val = params['entry'+itm.entry];
                    if(val)
                    {
                        var mval = attr=='discount'?val*-1:val;
                        tot = tot + mval;
                    }
                }
            }
        });
        return tot;
    }
    params.products = params.PRODUCTS = function()
    {
        var products = [];
        if(setting.currency)
        {
            var lines = params.getBill();
            products = lines.map(line=>{
                var [title, price, quantity, id, entry, amount, section] = line;
                return {title, price, quantity, id, entry, amount, section};
            })
            var getAmount = prd=>prd.amount||(Number(prd.price||0)*Number(prd.quantity||0));
            products.sort((a,b)=>getAmount(b)-getAmount(a));
        }
        return products;
    }
    params.topproduct = params.TOPPRODUCT = function()
    {
        var prds = params.products();
        return prds.length==0?'':prds[0].title;
    }
    params.gridscore = params.GRIDSCORE = function()
    {
        var args = Array.prototype.slice.call(arguments);
        var grdval = args.shift();
        var sel = args.shift();
        sel = Array.isArray(sel)?sel:[sel];
        var acc = 0;
        if(grdval && grdval.getMetadata)
        {
            var item = grdval.getMetadata();
            if(item && item.type=='GRID')
            {
                var rws = item.rows?item.rows:[];
                rws.forEach(function(rw, rwi){
                    if(sel.indexOf(rwi+1)>=0)
                    {
                        var valmap = {};
                        var val = entr[rw.entry];
                        val = Array.isArray(val)?val:[val];
                        val.forEach(function(vl){ valmap[vl] = vl; });
                        item.choices.forEach(function(ch, chi){
                            if(ch && valmap[ch.value])
                            {
                                var point = args[chi];
                                if(point) acc = acc + point;
                            }
                        });
                    }
                });
            }
        }
        return acc;
    }
    params.date = params.DATE = function(yy, mm, dd)
    {
        return toDate(new Date(yy, mm-1, dd));
    }
    params.datevalue = params.DATEVALUE = function(date)
    {
        if(date instanceof Date)
            return toDate(date);
        var vl = new String(date?date:'');
        var parsed = Date.parse(vl);
        if(isNaN(parsed)==false)
            return toDate(new Date(parsed));
    }
    params.today = params.TODAY = function()
    {
        var vl = new Date();
        return params.date(vl.getFullYear(), vl.getMonth()+1, vl.getDate());
    }
    params.now = params.NOW = function()
    {
        var vl = new Date();
        return toDate(vl);
    }
    params.datedif = params.DATEDIF = function(date1, date2, metric)
    {
        var start = new Date(date1.getTime());
        start.setHours(0);
        start.setMinutes(0, 0, 0);
        var end = new Date(date2.getTime());
        end.setHours(0);
        end.setMinutes(0, 0, 0);
        if(metric=='Y')
        {
            var years = end.getFullYear()-start.getFullYear();
            if(end.getMonth()<start.getMonth()||(end.getMonth()==start.getMonth()&&end.getDate()<start.getDate())) years = years-1;
            return years;
        }
        else if(metric=='M')
        {
            var months = (end.getFullYear()*12+end.getMonth())-(start.getFullYear()*12+start.getMonth());
            if(end.getDate()<start.getDate()) months = months-1;
            return months;
        }
        if(metric=='D')
        {
            return Math.round((end.getTime()-start.getTime())/(1000*60*60*24));
        }
        else if(metric=='h')
        {
            return Math.round((date2.getTime()-date1.getTime())/(1000*60*60));
        }
        else if(metric=='m')
        {
            return Math.round((date2.getTime()-date1.getTime())/(1000*60));
        }
        else if(metric=='s')
        {
            return Math.round((date2.getTime()-date1.getTime())/1000);
        }
    }
    params.WEEKDAY = params.weekday = function(dt, type=1)
    {
        return toDate(dt).day() - (type-1);
    }
    params.duplicate = params.DUPLICATE = function(url='')
    {
        var pairs = [];
        var en = entr?entr:{};
        for(var enid in en)
        {
            if(isNaN(enid)==false)
            {
                var enval = en[enid];
                enval = Array.isArray(enval)?enval:[enval];
                enval.forEach(function(enitm){
                    if(enitm=='__other_option__')
                        enitm = entr[enid+'-other_option_response'];
                    if(enitm)
                        pairs.push('entry.'+enid+'='+encodeURIComponent(enitm));
                });
            }
        }
        return url+'?'+pairs.join('&');
    }
    params.editurl = params.EDITURL = function()
    {
        var {userId, publishId} = request.params||{};
        var {savedId, owner} = draft;
        var {currency} = setting;
        var {shortId} = config;
        if(savedId)
        {
            var host = currency?'neartail.com':'formfacade.com';
            var url = `https://${host}/public/${userId||owner}/home/form/${publishId}?restoreId=${savedId}`;
            if(shortId) url = `https://${currency?'neartail.com':'formfacade.com'}/rs/${shortId}/${savedId}`;
            return url;
        }
    }
    params.prefilllink = params.PREFILLLINK = function()
    {
        var {userId, publishId} = request.params||{};
        var {savedId, owner, name} = draft;
        var {currency} = setting;
        var {shortId} = config;
        if(savedId && name)
        {
            var host = currency?'neartail.com':'formfacade.com';
            var url = `https://${host}/public/${userId||owner}/home/form/${publishId}?prefillId=${name}`;
            if(shortId) {
                var domain;
                if(fac && fac.hipaache)
                    domain = 'formesign.com';
                else if(currency)
                    domain = 'neartail.com';
                else
                    domain = 'formfacade.com';
                url = `${domain}/sm/${shortId}/${name}`;
            }
            return url;
        }
    }
    params.edit = params.EDIT = function(label)
    {
        var editurl = params.editurl();
        if(editurl)
        {
            var caption = setting.currency?'Edit Order':'Edit Response';
            return `
                <a class="card-link" href='${editurl}'>
                    ${label||caption}
                </a>
            `;
        }
        return '';
    }
    params.row = params.ROW = function()
    {
        var args = Array.prototype.slice.call(arguments);
        var width = Math.round(100/args.length);
        var htm = '<table class="ff-html-row" cellspacing="0" style="width:100%; border-spacing:0; border-collapse:collapse;"><tr>';
        args.forEach(function(cell, c){
            var align = c==0?'left':(c+1==args.length?'right':'center');
            if(args.length==1) align = 'center';
            var cells = Array.isArray(cell)?cell:[cell];
            htm += '<td style="width:'+width+'%; text-align:'+align+';">'+cells.join('<br/>')+'</td>';
        });
        htm += '</tr></table>';
        return htm;
    }
    params.chart = params.CHART = function()
    {
        var {userId, publishId} = request.params||{};
        var {savedId, owner} = draft;
        var args = Array.prototype.slice.call(arguments);
        var nm = args.shift();
        var url = `https://formfacade.com/chart/${publishId}/v1/${savedId}?type=${nm}`;
        args.forEach((val,v)=>{
            if(val && val.getMetadata)
            {
                var {title} = val.getMetadata()||{};
                var num = isNaN(val)?0:Number(val);
                url += '&'+encodeURIComponent(title)+'='+num;
            }
            else
            {
                var num = val?(isNaN(val)?0:Number(val)):0;
                url += '&'+encodeURIComponent(`Untitled ${v+1}`)+'='+num;
            }
        });
        return `<img src="${url}">`;
    }
    params.tag = params.TAG = function(nm, attr)
    {
        attr = typeof attr==='string'||attr instanceof String?{content:attr}:attr;
        var vl = attr.content; delete attr.content;
        var attrs = Object.keys(attr).map(function(anm){ return anm+'="'+attr[anm]+'"'; }).join(' ');
        return '<'+nm+' '+attrs+'>'+(vl?vl:'')+'</'+nm+'>';
    }
    Array('img','h1','h2','h3','h4','h5','h6','p','b','em','i','small','a','hr','s').forEach(function(tg){
        params[tg] = params[tg.toUpperCase()] = function(attr){ return params.tag(tg, attr); };
    });
    Array('ol','ul').forEach(function(tg){ 
        params[tg] = params[tg.toUpperCase()] = function(){
            var itms = Array.prototype.slice.call(arguments);
            var lst = itms.map(function(itm){ return '<li>'+itm+'</li>'; }).join('\n');
            return params.tag(tg, lst);
        }; 
    });
    params.hyperlink = params.HYPERLINK = function(url, label)
    {
        return '<a href="'+url+'" target="_blank">'+label+'</a>';
    }
    params.cta = params.CTA = function(url, label, newwindow)
    {
        var ctaclick = newwindow?`window.open(${JSON.stringify(url)})`:`location.href="${url}"`;
        return `<p class="ctabutton">
            <button type="button" class="btn btn-lg btn-primary ff-next" onclick='${ctaclick}'>
                ${label||'Proceed'}
            </button>
        </p>`;
    }
    params.jsa = params.JSA = function(js, label)
    {
        Object.entries(params).forEach(en=>{
            var [nm, vl] = en;
            if(nm.startsWith('entry') && js.indexOf(nm)>=0)
                js = js.split(nm).join(JSON.stringify(vl));
            else if(js.indexOf(nm+'()')>=0)
                js = js.split(nm+'()').join(JSON.stringify(vl()));
        });
        return `<p class="ctabutton">
            <button type="button" class="btn btn-lg btn-primary ff-next" onclick='eval(${JSON.stringify(js)})'>
                ${label||'Proceed'}
            </button>
        </p>`;
    }
    params.field = params.FIELD = function(attr)
    {
        var map = fac.mapping||{};
        var iid = map[attr];
        var itm = items[iid]||{};
        return params['entry'+itm.entry];
    }
    params.getAmount = function(amt)
    {
        if(!amt) amt = params.field('net-amount');
        if(!amt) amt = params.field('amount');
        if(amt && isNaN(amt)==false) return params.round(Number(amt));
    }
    params.getTitle = _=>scraped.title
    params.getBrand = _=>config.title||scraped.title
    params.getTxNumber = _=>params.orderId()||'000000'
    params.getTxNote = _=>'Order'+params.getTxNumber()
    params.renderWallet = function(wallet, rcp, amt)
    {
        var amflt = params.getAmount(amt);
        return `<div id="walletpane-${wallet}" class="walletpane" data-wallet="${wallet}" data-to="${rcp}" data-amount="${amflt}">
                  <h3>Loading...</h3>
                </div>`;
    }
    params.upi = params.UPI = (rcp, amt)=>params.renderWallet('upi', rcp, amt);
    params.venmo = params.VENMO = (rcp, amt)=>params.renderWallet('venmo', rcp, amt);
    params.cashapp = params.CASHAPP = (rcp, amt)=>params.renderWallet('cashapp', rcp, amt);
    params.paypal = params.PAYPAL = (rcp, amt)=>params.renderWallet('paypal', rcp, amt);
    params.paynow = params.PAYNOW = (rcp, amt)=>params.renderWallet('paynow', rcp, amt);
    params.code = params.CODE = function(nm)
    {
        return params[nm].toString();
    }
    params.sequence = params.SEQUENCE = function(nm)
    {
        if(curr.draft)
        {
            var seq = nm=='submitted'?curr.draft.submitSeq:curr.draft.draftSeq;
            if(seq && isNaN(seq)==false) return parseInt(seq);
        }
        return 0;
    }
    params.orderId = params.ORDERID = _=>params.sequence('submitted');
    params.center = params.CENTER = html=>`<div class="ff-cta-center">${html}</div>`;
    params.forum = _=>{
        var prd = (entr[488229902]||'all').toLowerCase();
        var slug = (entr[1380645483]||'untitled').trim().toLowerCase();
        if(slug.length>50) slug = slug.slice(0,50);
        slug = slug.replace(/\s+/g, "-").replace(/[^\w\-]+/g, "").replace(/\-\-+/g, "-").replace(/^-+/, "").replace(/-+$/, "");
        return `//near.tl/support/forum/${prd}/${slug||'untitled'}.${draft.savedId}.html?nocache=${new Date().getTime()}`;
    };
    params.showlocation = params.SHOWLOCATION = function()
    {
        var fld = citm||{};
        if(fld.type=="PARAGRAPH_TEXT" || fld.type=="TEXT")
        {
            return `
                <a class="card-link" href="javascript:void(0)"
                    onclick="formFacade.getLocation(document.getElementById('Widget${fld.id}'))">
                    Show current location
                </a>
            `;
        }
        return 'Works only with Text field';
    }
    params.addtocalendar = params.ADDTOCALENDAR = function(start, end, subject, body, location)
    {
        var startdt = toDate(start||new Date());
        var enddt = end?toDate(end):startdt.add(1, 'hours');
        if(!subject) subject = params.getTitle();
        formatOffset = offsetInMins=>{
            var negative=offsetInMins<0?"-":"";
            var positiveMins= Math.abs(offsetInMins);
          
            var hours   = Math.floor(positiveMins/ 60);
            var mins = Math.floor((positiveMins- ((hours * 3600)) / 60));
            if (hours   < 10) {hours   = "0"+hours;}
            if (mins < 10) {mins = "0"+mins;}
         
            return negative+hours+':'+mins;
        };
        var caldef = {
            google:{
                action:'TEMPLATE', text:subject, details:body, location:location,
                dates:startdt.toISOString().replace(/[^\w\s]/gi, '')+'/'+enddt.toISOString().replace(/[^\w\s]/gi, '') 
            },
            outlook:{
                rru:'addevent', subject:subject, body:body, location:location,
                startdt:startdt.toISOString().split('.')[0]+formatOffset(startdt.getTimezoneOffset()), 
                enddt:enddt.toISOString().split('.')[0]+formatOffset(enddt.getTimezoneOffset())
            },
            ics:{
                BEGIN:"VCALENDAR",
                CALSCALE:"GREGORIAN",
                METHOD:"PUBLISH",
                PRODID:"-//Neartail-Formfacade//Forms",
                VERSION:"2.0",
                "BEGIN ":"VEVENT",
                UID:"Formfacade-"+draft.savedId,
                "DTSTAMP;VALUE=DATE":new Date().toISOString().split('.')[0].replace(/[^\w\s]/gi, ''),
                "DTSTART;VALUE=DATE":startdt.toISOString().split('.')[0].replace(/[^\w\s]/gi, ''),
                "DTEND;VALUE=DATE":enddt.toISOString().split('.')[0].replace(/[^\w\s]/gi, ''),
                SUMMARY:subject,
                DESCRIPTION:body,
                LOCATION:location,
                END:"VEVENT",
                "END ":"VCALENDAR"
            }
        };
        var calqry = Object.fromEntries(Object.entries(caldef).map(calen=>{
            var [calnm, calvl] = calen;
            var SEPARATOR = '&';
            if(calnm=='ics') SEPARATOR = (navigator.appVersion.indexOf('Win') !== -1) ? '\r\n' : '\n';
            caljoin = Object.entries(calvl).map(cal=>{
                var [nm, vl] = cal;
                if(vl)
                {
                    if(calnm=='ics')
                        return nm.trim()+':'+vl;
                    else
                        return nm+'='+encodeURIComponent(vl);
                }
            }).filter(cal=>cal).join(SEPARATOR);
            return [calnm, caljoin];
        }));
        return `<p class="ff-cta-calendar">
                    <button type="button" class="btn btn-lg btn-primary ff-next"
                        onclick='window.open(window.URL.createObjectURL(new Blob([${JSON.stringify(calqry.ics)}], {type:"text/calendar"})));'>
                        Add to your calendar
                    </button>
                </p>
                <p>
                    <a class="card-link" target="_blank"
                        href="https://calendar.google.com/calendar/render?${calqry.google}">
                        Google
                    </a>
                    <a class="card-link" target="_blank"
                        href="https://outlook.office.com/calendar/0/deeplink/compose?${calqry.outlook}">
                        Outlook
                    </a>
                    <a class="card-link" target="_blank"
                        href="https://outlook.office.com/calendar/0/deeplink/compose?${calqry.outlook}">
                        Office 365
                    </a>
                </p>`;
    }
    params.signature = params.SIGNATURE = function(url, style)
    {
        if(url && /https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)/.test(url)){
            var now = new Date().getTime();
            return '<img src="'+url+'" style="'+style+'"></img>';
        } else
            return ''
    }
    params.def = _=>params;
    const names = Object.keys(params);
    const vals = Object.values(params);
    if(opts.returntype)
    {
        try
        {
            if(tmpl.startsWith('${') && tmpl.endsWith('}')) tmpl = tmpl.substring(2, tmpl.length-1);
            var calcrs = new Function(...names, 'return '+tmpl)(...vals);
            return calcrs;
        }
        catch(err)
        {
            console.trace(err, 'Computation failed for '+tmpl);
            return err;
        }
    }
    else
    {
        try
        {
            var retmpl = tmpl.replace(new RegExp('`', 'g'), '\\`');
            var calcrs = new Function(...names, `return \`${retmpl}\`;`)(...vals);
            return calcrs;
        }
        catch(err)
        {
            console.trace(err, 'Computation failed for '+tmpl);
            return 'Computation failed for '+tmpl+' due to '+err;
        }
    }
}



window.formFacade = new FormFacade({"request":{"params":{"publishId":"1FAIpQLSfzcLCrC9cBOpqJIyYOQuFIYRixlvplwIsaqMWYQfCtNmyfQg","target":"classic","userId":"106758614078420831012"},"query":{"div":"ff-compose"}}});

formFacade.template = {"style":"<% \n  const pSBC=(p,c0,c1,l)=>{\n    let r,g,b,P,f,t,h,m=Math.round,a=typeof(c1)==\"string\";\n    if(typeof(p)!=\"number\"||p<-1||p>1||typeof(c0)!=\"string\"||(c0[0]!='r'&&c0[0]!='#')||(c1&&!a))return null;\n    h=c0.length>9,h=a?c1.length>9?true:c1==\"c\"?!h:false:h,f=pSBC.pSBCr(c0),P=p<0,t=c1&&c1!=\"c\"?pSBC.pSBCr(c1):P?{r:0,g:0,b:0,a:-1}:{r:255,g:255,b:255,a:-1},p=P?p*-1:p,P=1-p;\n    if(!f||!t)return null;\n    if(l)r=m(P*f.r+p*t.r),g=m(P*f.g+p*t.g),b=m(P*f.b+p*t.b);\n    else r=m((P*f.r**2+p*t.r**2)**0.5),g=m((P*f.g**2+p*t.g**2)**0.5),b=m((P*f.b**2+p*t.b**2)**0.5);\n    a=f.a,t=t.a,f=a>=0||t>=0,a=f?a<0?t:t<0?a:a*P+t*p:0;\n    if(h)return\"rgb\"+(f?\"a(\":\"(\")+r+\",\"+g+\",\"+b+(f?\",\"+m(a*1000)/1000:\"\")+\")\";\n    else return\"#\"+(4294967296+r*16777216+g*65536+b*256+(f?m(a*255):0)).toString(16).slice(1,f?undefined:-2)\n  }\n\n  pSBC.pSBCr=(d)=>{\n    const i=parseInt;\n    let n=d.length,x={};\n    if(n>9){\n      const [r, g, b, a] = (d = d.split(','));\n            n = d.length;\n      if(n<3||n>4)return null;\n      x.r=i(r[3]==\"a\"?r.slice(5):r.slice(4)),x.g=i(g),x.b=i(b),x.a=a?parseFloat(a):-1\n    }else{\n      if(n==8||n==6||n<4)return null;\n      if(n<6)d=\"#\"+d[1]+d[1]+d[2]+d[2]+d[3]+d[3]+(n>4?d[4]+d[4]:\"\");\n      d=i(d.slice(1),16);\n      if(n==9||n==5)x.r=d>>24&255,x.g=d>>16&255,x.b=d>>8&255,x.a=Math.round((d&255)/0.255)/1000;\n      else x.r=d>>16,x.g=d>>8&255,x.b=d&255,x.a=-1\n    }return x\n  };\n  const lighten = (p,c0,c1,l)=>pSBC(p,c0,c1,l);\n\n  function hexToRGBA(hex, alpha) {\n    // Remove the # symbol if it exists\n    hex = hex.replace(/^#/, '');\n\n    // Parse the hex values for red, green, and blue\n    const r = parseInt(hex.substring(0, 2), 16);\n    const g = parseInt(hex.substring(2, 4), 16);\n    const b = parseInt(hex.substring(4, 6), 16);\n\n    // Ensure that alpha is within the 0 to 1 range\n    alpha = Math.min(1, Math.max(0, alpha));\n\n    // Create and return the RGBA string\n    return `rgba(${r}, ${g}, ${b}, ${alpha})`;\n  }\n\n%>\n<%\n  var {userId, publishId, target} = data.request.params||{};\n  var {form, scraped, facade} = data||{};\n  var fac = facade||{};\n  var isTailwindStyleIsActive = !(target=='bootstrap' || target =='gsuite' || target =='clean');\n  var themecolor = config.themecolor;\n  if(!themecolor)\n  {\n    if(fac.neartail || fac.whatsapp)\n      themecolor = 'minimal-77cde3';\n    else\n      themecolor = 'colorful-5d33fb';\n  }\n  var [themed, thmcolor] = themecolor.split('-');\n  var primary = '#'+thmcolor;\n  var secondary = '#ffffff';\n  if(fac.setting && fac.setting.primary)\n    primary = fac.setting.primary;\n  else if(config && config.theme)\n    primary = config[config.theme+'pri'];\n  if(fac.setting && fac.setting.secondary)\n    secondary = fac.setting.secondary;\n  else if(config && config.theme)\n    secondary = config[config.theme+'sec'];\n  else if (config.themeSecondary) {\n    secondary = config.themeSecondary;\n  }\n  var headfont;\n  if(config.theme)\n    headfont = config[config.theme+'head'];\n  else if(themed=='colorful')\n    headfont = 'Poppins';\n  else if(themed=='minimal')\n    headfont = 'Work Sans';\n  var parafont;\n  if(config.theme)\n    parafont = config[config.theme+'para'];\n  else if(themed=='colorful')\n    parafont = 'Roboto';\n  else if(themed=='minimal')\n    parafont = 'Work Sans';\n  var fontSize = 14;\n  if(config.theme)\n  {\n    var strsize = config[config.theme+'size'];\n    if(isNaN(strsize)==false) fontSize = parseInt(strsize);\n  }\n  if(fac.enhance)\n  {\n    var strsize = fac.enhance.fontSize;\n    if(strsize && isNaN(strsize)==false) fontSize = parseInt(strsize);\n  }\n  var formBgColor = '#ffffff';\n  var enhanceBg = null;\n  var themeBg = null;\n  var pageBgColor = null;\n\n  var isMinimalTheme = config && config.theme !== 'colorful';\n\n  if(isMinimalTheme) {\n    pageBgColor = secondary || '#f5f5f5';\n  } else {\n    pageBgColor = '#ffffff';\n  }\n\n  var isTransparent = false;\n\n  if(fac.enhance && fac.enhance.transparent === 'on')\n  {\n    isTransparent = true;\n  }\n  else if (config && config.formtransparent === 'on')\n  {\n    isTransparent = true;\n  }\n\n  if(fac.enhance && fac.enhance.background)\n  {\n    formBgColor = fac.enhance.background;\n    enhanceBg = fac.enhance.background;\n    if (config && config.formbgcolor) {\n      themeBg = config.formbgcolor;\n    }\n  }\n  else if (config && config.formbgcolor)\n  {\n    formBgColor = config.formbgcolor;\n    themeBg = config.formbgcolor;\n  }\n\n  <!-- NEW FORM -->\n  if(!enhanceBg && isMinimalTheme && isTransparent) {\n    formBgColor = secondary;\n  }\n\n  var isDarkTheme = isColorDark(\n    formBgColor\n  );\n\n  var isPageDarkTheme = isMinimalTheme ?  isColorDark(\n    pageBgColor\n  ) : isDarkTheme;\n  \n  var ifFlipTextColor = isDarkTheme;\n  var fontColor = ifFlipTextColor ? '#fafafa' : '#202124';\n  var field = isTailwindStyleIsActive ? 'transparent' : '#fff';\n  var fieldBorder = ifFlipTextColor ? '#808080' : 'rgb(0 0 0 / 15%)';\n  if(isDarkTheme) {\n    // lighten based on secondary if minimal theme else based on formBgColor.\n    field = 'rgb(255 255 255 / 8%)';\n    fieldBorder = 'rgb(255 255 255 / 7%)';\n  }\n  var borderColor = ifFlipTextColor ? 'rgb(255 255 255 / 25%)' : 'rgb(0 0 0 / 15%)';\n  var enhanceFontColor = null;\n  if(fac.enhance)\n  {\n    if(fac.enhance.fontColor){\n      fontColor = fac.enhance.fontColor;\n      enhanceFontColor = fac.enhance.fontColor;\n    }\n    if(fac.enhance.field && field != fac.enhance.field){\n      field = fac.enhance.field;\n    }\n  }\n\n  function isColorDark(hexColor) {\n    if(!hexColor) {\n      hexColor = '#ffffff';\n    }\n    // Convert the hex color to RGB\n    const r = parseInt(hexColor.slice(1, 3), 16);\n    const g = parseInt(hexColor.slice(3, 5), 16);\n    const b = parseInt(hexColor.slice(5, 7), 16);\n\n    // Calculate the relative luminance to determine darkness\n    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;\n\n    // You can adjust this threshold to fit your definition of dark/light\n    return luminance < 0.5;\n  }\n\n  function toHSL(hexa = '#ffffff') {\n    var r = parseInt(hexa.slice(1, 3), 16) / 255,\n      g = parseInt(hexa.slice(3, 5), 16) / 255,\n      b = parseInt(hexa.slice(5, 7), 16) / 255;\n\n    var max = Math.max(r, g, b),\n      min = Math.min(r, g, b);\n    var h,\n      s,\n      l = (max + min) / 2;\n\n    if (max === min) {\n      h = s = 0;\n    } else {\n      var d = max - min;\n      s = l > 0.5 ? d / (2 - max - min) : d / (max + min);\n\n      switch (max) {\n        case r:\n          h = ((g - b) / d + (g < b ? 6 : 0)) / 6;\n          break;\n        case g:\n          h = ((b - r) / d + 2) / 6;\n          break;\n        case b:\n          h = ((r - g) / d + 4) / 6;\n          break;\n      }\n    }\n    h = Math.round(h * 360);\n    s = Math.round(s * 100);\n    l = Math.round(l * 100);\n    return [h, s, l];\n} \n\n%>\n<% if(headfont){ %>\n  <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=<%=headfont%>:300,400,500,600,700,800\">\n<% } %>\n<% if(parafont !== headfont){ %>\n  <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=<%=parafont%>:300,400,500,600,700,800\">\n<% } %>\n<link href=\"https://fonts.googleapis.com/icon?family=Material+Icons\" rel=\"stylesheet\" media=\"screen\">\n<% \n  if(!fac) {\n    fac = {};\n  }\n  const rtlLangs = {\n    'ar': 'Arabic',\n    'he': 'Hebrew',\n    'fa': 'Persian',\n    'ur': 'Urdu',\n  }\n  var rtlLangText = fac.langtext || {};\n  var rtlLang = rtlLangText.language || '';\n  var facadeSetting = fac.setting || {};\n  var cl = facadeSetting.language || rtlLang || 'en';\n  var dir = cl in rtlLangs ? 'rtl' : 'ltr';\n%>\n<style>\n  <% if (dir === 'rtl') { %>\n  .ff-success {\n    direction: rtl;\n  }\n  #ff-cart-sidebar.active, #ff-search-sidebar.active {\n    right: unset;\n    left: 0;\n    direction: rtl;\n  }\n  <% } %>\n  <%\n    var enhance = fac.enhance || {};\n    if(!config) {\n      config = {};\n    }\n  %>\n\n  <% if(isTransparent) { %>\n    /* In case Editor, add background color. */\n    <% if(isEditMode()) { %>\n    .ff-form {\n      background: <%-formBgColor%> !important;\n    }\n    <% } %>\n  <% } %>\n  /* IF COLORFUL THEME (since the pageBgColor = formBgColor) */\n  <% if (!isMinimalTheme) { %>\n    #ff-formpage-body {\n      background-color: <%-formBgColor%> !important;\n    }\n  <% } else { %>\n    body#ff-formpage-body {\n      background-color: <%=pageBgColor%> !important;\n    }\n  <% } %>\n  <% if (isTransparent && isMinimalTheme) { %>\n    #pageform #ff-compose {\n      background: var(--ff-formbgcolor) !important\n    }\n    /* if transparency enabled then reduce the margin-top for the minimal theme. */\n    @media (min-width: 651px) {\n      .page-wrapper #pageform #ff-compose {\n        margin-top: -50px !important;\n      }\n    }\n  <% } %>\n  /* If DarkTheme and IsMinimal and enhanceBg should not be available (Only for new form which does not have enhance bg.) */\n  <% if (isMinimalTheme && isPageDarkTheme && !enhanceBg) { %>\n    .page-wrapper header.navbar.navbar-sticky {\n      background-color: <%-pageBgColor%>;\n      color: #FFFFFF;\n      border-bottom: 1px solid rgb(255 255 255 / 15%) ;\n    }\n    .page-wrapper .cart-btn i, .page-wrapper .ff-cart-icon, a.site-logo.visible-mobile.ff-logo, a.site-logo.visible-desktop.ff-logo {\n      color: #FFFFFF;\n    }\n    .ff-image-list__image {\n      border: 0px;\n      background-color: <%-field%>\n    }\n    #ff-addprd-popup .ff-prdimg-thumbnail {\n      border: 0px;\n    }\n  <% } %>\n  <%\n    var hslOfPrimary = toHSL(primary);\n    var secondaryHeading = \"#4B4B4B\", secondaryPara = \"#8E8E8E\", secondaryGray = \"#e2e2e2\";\n    if(isPageDarkTheme) {\n      secondaryHeading = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 96%)`;\n      secondaryPara = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 80%)`;\n      secondaryGray = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 20%)`;\n    } else if (formBgColor !== '#ffffff' && formBgColor !== '#FFFFFF') {\n      secondaryHeading = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 4%)`;\n      secondaryPara = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 20%)`;\n      secondaryGray = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 80%)`;\n    }\n  %>\n  <%  if(isPageDarkTheme) { %>\n    #ff-addprd-overlay {\n      background-color: #575c6885 !important;\n    }\n  <%  } %>\n  <%  if(isMinimalTheme && isPageDarkTheme) { %>\n    .page-wrapper .footer .column {\n      color: #fafafa !important;\n      border-top: 1px solid rgb(255 255 255 / 15%) !important;\n    }\n    .page-wrapper .footer * {\n      color: <%=`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 96%)`%>;\n    }\n  <% } %>\n  <%\n    var popupBgColor = enhanceBg ? '#ffffff' : formBgColor;\n    var popupFontColor = enhanceBg ? '#202124' : fontColor;\n    var popupBorderColor = enhanceBg ? '#d2d2d2' : borderColor;\n\n    if(isMinimalTheme && !enhanceBg) {\n      popupBgColor = isPageDarkTheme ? pageBgColor : '#ffffff';\n      popupFontColor = isPageDarkTheme ? '#fafafa' : '#202124';\n      popupBorderColor = isPageDarkTheme ? 'rgb(255 255 255 / 25%)' : 'rgb(0 0 0 / 15%)';\n    }\n  %>\n  /* Hide scrollbar for ff-mode = preview */\n  <%\n  var urlparams = window.location&&window.location.search&&new URLSearchParams(window.location.search);\n  if(urlparams && urlparams.get('ff-mode')=='preview'){ \n  %>\n    ::-webkit-scrollbar {\n      display: none;\n    }\n  <% } %>\n  button#ff-submit-root {\n\t\tvisibility: visible;\n\t\tposition: initial;\n\t\twidth: auto;\n\t}\n  :root {\n    --ff-fs: <%=fontSize%>px;\n    <% \n      var fontDiff = (fontSize - 16) * 0.05;\n      var fontScale = 1 + fontDiff;\n    %>\n    --ff-scale:<%-fontScale%>;\n    <% if(target=='wordpress' && false){ %>\n    --ff-primary-color: var(--wp--preset--color--primary, <%-primary%>);\n    <% } else{ %>\n    --ff-primary-color: <%-primary%>;\n    <% } %>\n    <%\n      var shades = {\n        10:.8, 50:.6, 100:.5, 200:.4, 300:.3, 400:.2, 500:.1, \n        600:0, 700:-.1, 800:-.2, 900:-.3, 1000:-.4\n      };\n\t\t  for(var shade in shades) { \n\t\t\tvar key = `--ff-primary-${shade==1000?950:shade}`;\n      var color = lighten(shades[shade], primary);\n    %>\n    <%-key%>: <%-color%>;\n    <% } %>\n    --ff-primary-light: <%-toRGB(primary, 0.4)%>;\n    --ff-bgcolor:<%-formBgColor%>;\n    --popup-bgcolor:<%-popupBgColor%>;\n    --popup-fontcolor:<%-popupFontColor%>;\n    --popup-bordercolor:<%-popupBorderColor%>;\n    --ff-formbgcolor: <%-isTransparent ? 'transparent' : formBgColor%>;\n    --ff-font-color:<%-enhanceFontColor ? enhanceFontColor : '#202124'%>;\n    --ff-gray-900:<%-fontColor%>;\n    --ff-gray-400:<%-ifFlipTextColor ? '#808080' : 'rgb(0 0 0 / 15%)'%>;\n    --ff-secondary-para: <%-secondaryPara%>;\n    --ff-secondary-heading: <%-secondaryHeading%>;\n    --ff-secondary-gray: <%-secondaryGray%>;\n    <% \n      var minFontSizes = {'00':.8, 0:.9, 1:1, 2:1.1, 3:1.25, 4:1.5, 5:2, 6:2.5, 7:3, 8:3.5};\n      for(var s in minFontSizes){\n        var fontSize = Math.round(14*minFontSizes[s]*10)/10;\n    %>\n      --ff-font-size-<%-s%>:max(calc(var(--font-size-<%-s%>)*var(--ff-scale)), <%-fontSize%>px);\n    <% } %>\n    <% \n      var minSizes = {'000':-.5, '00':-.25, 1:.25, 2:.5, 3:1, 4:1.25, 5:1.5, \n        6:1.75, 7:2, 8:3, 9:4, 10:5, 11:7.5, 12:10, 13:15, 14:20, 15:30};\n      for(var s in minSizes){\n        var size = Math.round(14*minSizes[s]*10)/10;\n    %>\n      --ff-size-<%-s%>:max(calc(var(--size-<%-s%>)*var(--ff-scale)), <%-size%>px);\n    <% } %>\n    <%\n    if(formBgColor === 'transparent')\n    {\n      field = 'transparent';\n    }\n    var hslOfFormBgColor = toHSL(formBgColor);\n    var accentColor = primary;\n    var activeItemColor = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 80%)`;\n    var primaryBtnColor = lighten(-0.145, primary);\n    var hslOfPrimaryBtnColor = toHSL(primaryBtnColor);\n    if(hslOfPrimaryBtnColor[2] <= 10 || hslOfPrimaryBtnColor[2] >= 65) {\n      primaryBtnColor = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 20%)`;\n    }\n    if((hslOfPrimary[2] <= 20 || hslOfPrimary[2] > 55) && primary !== '#000000') {\n      activeItemColor = `hsl(${hslOfPrimary[0]}, 75%, 80%)`;\n    }\n    /* if transparent and primary color is same background and is minimal theme and config.theme exist. */\n    if(config && config.theme && isMinimalTheme && formBgColor === primary && isTransparent) {\n      primaryBtnColor = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 40%)`;\n    }\n\n    if(config && config.theme && !isMinimalTheme && formBgColor === primary) {\n      primaryBtnColor = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 40%)`;\n    }\n\n    var isPrimaryColorDark = isColorDark(primary);\n    if(!isDarkTheme) {\n      fieldBorder = borderColor = popupBorderColor = 'rgb(0 0 0 / 12%)';\n    } else if (isDarkTheme) {\n      fieldBorder = 'rgba(255, 255, 255, 0.15)';\n    }\n    if(!isDarkTheme && isPageDarkTheme && !enhanceBg) {\n      popupBorderColor = 'rgba(255, 255, 255, 0.15)';\n    }\n    if(!isPrimaryColorDark) {\n      accentColor = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 35%)`;\n    } else {\n      accentColor = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 30%)`;\n    }\n    if(field === 'transparent' && formBgColor !== '#FFFFFF' && formBgColor !== '#ffffff') {\n      field = `rgb(0 0 0 / 4%)`;\n    }\n    var aHrefColor = primaryBtnColor;\n    /* if background is dark then need the light color  */\n    var isFormBgColorDark = isColorDark(formBgColor);\n    if(isFormBgColorDark) {\n      aHrefColor = `hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 90%)`;\n    }\n    var signatureBgColor = field;\n    if(isFormBgColorDark) {\n      signatureBgColor = 'rgb(255 255 255 / 18%)';\n    }\n    %>\n    /* Box shadow color for dark theme. */\n    <% if(isPageDarkTheme && config && config.theme) { %>\n    --ff-primary-300: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 17.5%)`;%>; \n    <% } %>\n    --ff-font-size:var(--ff-font-size-1);\n    --ff-head-size:var(--ff-font-size-5);\n    --ff-font-small:var(--ff-font-size-0);\n    --ff-field-bgcolor:<%-field%>;\n    --ff-signature-bg: <%-signatureBgColor%>;\n    --ff-field-border: <%-fieldBorder%>;\n    --ff-heading-font:<%-headfont?JSON.stringify(headfont):'inherit'%>;\n    --ff-paragraph-font:<%-parafont?JSON.stringify(parafont):'inherit'%>;\n    --ff-gray-200: <%= borderColor %>;\n    --ff-placeholder: <%= ifFlipTextColor ? '#e6e6e6' : '#333333' %>;\n    --ff-primary-950: <%= accentColor %> !important;\n    --ff-primary-50: <%= activeItemColor %> !important;\n    --popup-bordercolor: <%= popupBorderColor %> !important;\n    --ff-primary-700: <%= primaryBtnColor %> !important;\n    --ff-href-color: <%= aHrefColor %> !important;\n  }\n  <% if(config && config.theme && !isMinimalTheme && formBgColor === primary) { %> \n    #ff-formpage-body .footer {\n      border-top: 1px solid rgb(255 255 255 / 5%);\n    }\n  <% } %>\n  <%  if(config && config.theme && !isMinimalTheme && isDarkTheme) { %>\n\t\t#ff-formpage-body .ff-footer-svg {\n\t\t\topacity: 0.055 !important;\n\t\t}\n\t<% } %>\n  <% if(!isMinimalTheme && !isPrimaryColorDark) { %>\n    #ff-formpage-body span.ff-cart-count.count {\n      color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 35%)`%>;\n      font-weight: 600 !important;\n    }\n    #ff-formpage-body h3.text-white.display-4 {\n      color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 8%)`%> !important;\n    }\n    #ff-formpage-body .btn-white-border {\n      color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 25%)`%> !important;\n      border-color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 25%)`%> !important;\n    }\n    #ff-formpage-body #navbar-expand .nav-item a {\n      color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 25%)`%> !important;\n    }\n    @media (min-width: 992px) {\n      #ff-formpage-body .navbar-brand  {\n        color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 25%)`%> !important;\n      }\n    }\n    #ff-formpage-body #navbar-scroll .nav-item a {\n      color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 25%)`%> !important;\n    }\n    #ff-formpage-body .sticky-active .navbar-brand  {\n      color: #ffffff !important;\n    }\n    #ff-formpage-body .sticky-active #navbar-scroll .nav-item a {\n      color: #ffffff !important;\n    }\n    #ff-formpage-body .footer span {\n      color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 10%)`%> !important;\n    }\n    #ff-formpage-body .mouse-down a {\n      color: <%-`hsl(${hslOfPrimary[0]}, ${hslOfPrimary[1]}%, 25%)`%> !important;\n    }\n  <% } %>\n  <% if (isTailwindStyleIsActive) { %>\n  .ff-form, .ff-form div, .ff-form p, #ff-addprd-popup {\n    font-size: var(--ff-fs);\n  }\n  <% } %>\n  <% if (isDarkTheme && isTailwindStyleIsActive) { %> \n  .ff-form input[type=\"datetime-local\"]::-webkit-calendar-picker-indicator, .ff-form input[type=\"time\"]::-webkit-calendar-picker-indicator, .ff-form input[type=\"date\"]::-webkit-calendar-picker-indicator {\n    filter: invert(0.8);\n    cursor: pointer;\n    opacity: 0.9;\n  }\n  .ff-form input[type=\"datetime-local\"]::-webkit-calendar-picker-indicator:hover, .ff-form input[type=\"time\"]::-webkit-calendar-picker-indicator:hover, .ff-form input[type=\"date\"]::-webkit-calendar-picker-indicator:hover {\n    opacity: 1;\n  }\n  <% } %>\n  .ff-public-mode .ff-editwidget{ display:none !important; }\n  .ff-public-mode .ff-edittheme{ display:none !important; }\n  .ff-public-mode .ff-editsection{ display:none !important; }\n\n  /* Custom CSS */\n  <% if(fac.enhance && fac.enhance.css){ %>\n    <% if(typeof(formFacade) == 'undefined' || !formFacade.removeCustomCSSInEditor()) { %>\n      <%-fac.enhance.css%>\n    <% } %>\n  <% } %>\n</style>\n\n<%\n  var fcitms = fac.items?Object.values(fac.items):[];\n  var fcfiles = fcitms.filter(function(itm){ return itm.type=='FILE_UPLOAD'; });\n  if(fcfiles.length>0){\n    var lng;\n    if(config && config.language) lng = config.language;\n    if(fac.setting && fac.setting.language) lng = fac.setting.language;\n    var loc = lng&&langtext?langtext.locale:null;\n    if(lng && loc)\n    {\n      loc = loc.indexOf('_')>0?loc:(lng+'_'+loc);\n      loadScript('https://releases.transloadit.com/uppy/v3.7.0/uppy.min.js', function(){\n        loadScript('https://releases.transloadit.com/uppy/locales/v3.0.7/'+loc+'.min.js', function(){ formFacade.renderUpload(loc); });\n      });\n    }\n    else\n    {\n      loadScript('https://releases.transloadit.com/uppy/v3.7.0/uppy.min.js', function(){ formFacade.renderUpload(loc); });\n    }\n%>\n    <link href=\"https://releases.transloadit.com/uppy/v3.7.0/uppy.min.css\" rel=\"stylesheet\">\n<% } %>\n<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/@fancyapps/ui@5.0/dist/fancybox/fancybox.css\" />\n<%\nif(fac && fac.setting && fac.setting.currency) {\n  loadScript(\"https://cdn.jsdelivr.net/npm/@fancyapps/ui@5.0/dist/fancybox/fancybox.umd.js\", () => {\n    if(typeof(Fancybox) == 'undefined') {\n      return;\n    }\n    Fancybox.bind('[data-fancybox]', {\n      compact: false,\n      contentClick: \"iterateZoom\",\n      Images: {\n          Panzoom: {\n              maxScale: 3,\n          },\n      },\n      Toolbar: {\n          display: {\n              left: [\n                  \"infobar\",\n              ],\n              middle: [],\n              right: [\n                  \"iterateZoom\",\n                  \"fullscreen\",\n                  \"close\",\n              ],\n          }\n      },\n      Thumbs : {\n        type: \"classic\"\n      }\n    });\n  });\n}\n%>\n<%\n  var hasFormEsign = fac && fac.formesign;\n  var fcitms = fac.items?Object.values(fac.items):[];\n  var fcSignature = fcitms.filter(function(itm){ return itm.subtype=='SIGNATURE'; });\n  if(fcSignature.length>0 || hasFormEsign){\n    loadScript(\"https://cdn.jsdelivr.net/npm/signature_pad@4.0.5/dist/signature_pad.umd.min.js\", function(){ formFacade && formFacade.renderSignature(); });\n  }\n%>\n","text":"<%\n  var params = data.request.params;\n  var id = params.publishId;\n  var pubfrm = data.form||{};\n  var frm = data.scraped||{};\n  var fac = data.facade||{};\n  if(!fac.info) fac.info = {};\n  if(!fac.setting) fac.setting = {};\n  if(!fac.gpt) fac.gpt = {};\n  var frmitms = frm.items||{};\n  var fcitms = fac.items||{};\n  var unit = 'kg';\n  if(fac.setting.currencyCode=='USD')\n    unit = 'lb';\n  var enhance = fac.enhance||{};\n  var layout = enhance.layout||'default';\n  draft = draft||{};\n  if(!draft.entry) draft.entry = {};\n  var officeUseSections = fac.setting.officeUseSections||{};\n  var officeUseSectionIds = Object.values(officeUseSections);\n  var sections = getSections();\n  var frstsec = sections[0]||{};\n  var frstitms = frstsec.items||[];\n  var frstfcitms = frstitms.map(itm=>fcitms[itm.id]||{});\n  var pwdfcitms = frstfcitms.filter(itm=>itm.alias=='password');\n  var hasPassword = fac.gpt.flag=='phishing'&&frstitms.length<5&&pwdfcitms.length>=1;\n  var cardfcitms = Object.values(fcitms).filter(itm=>itm.alias=='card');\n  var cvvfcitms = Object.values(fcitms).filter(itm=>itm.alias=='cvv');\n  var hasCVV = fac.gpt.flag=='phishing'&&cardfcitms.length>=1&&cvvfcitms.length>=1;\n  var flagFormPhishing = (fac.gpt.flagForm=='phishing');\n  if(fac.setting.flag == 'safe')\n    flagFormPhishing = false;\n  var flag = hasPassword||hasCVV?'phishing':null;\n  flag = fac.setting.flag||flag;\n  var reurl = 'https://formfacade.com/website/customize-google-forms.html?product=website';\n  var createFormURL = 'https://formfacade.com/signup/onboard/website';\n  if(fac.formesign || fac.hipaache || fac.formprefill || fac.formfillable || fac.mailrecipe)\n  {\n    reurl = 'https://formesign.com/esign/esignature-google-forms.html?product=esign';\n    createFormURL = 'https://formesign.com/signup/onboard/esign';\n  }\n  else if(fac.neartail || fac.whatsapp)\n  {\n    reurl = 'https://neartail.com/order-form/create-order-form.html?product=order-form';\n    createFormURL = 'https://neartail.com/signup/onboard/order-form';\n  }\n  createFormURL += '?utm_source=thankyou&utm_medium='+params.userId+'&utm_campaign='+params.publishId+'&title='+encodeURIComponent((frm && frm.title) ? frm.title : \"\");\n  reurl += '&utm_source=madewith&utm_medium='+params.userId+'&utm_campaign='+params.publishId;\n  reurl += '&plan='+(config.plan||'free')+'&userId='+params.userId;\n  if(config && config.title) reurl += '&by='+encodeURIComponent(config.title);\n\n  var trg = params.target;\n  var prepend = clnm=>trg=='bootstrap'||trg=='gsuite'||trg=='clean'?clnm:('rest-'+clnm);\n  var backlst = ['btn', 'btn-lg', 'btn-secondary'].map(cls=>prepend(cls));\n  var submitlst = ['btn', 'btn-lg', 'btn-primary'].map(cls=>prepend(cls));\n  if(isEditMode())\n  {\n    backlst.push('creator-blur');\n    submitlst.push('creator-blur');\n  }\n  var backcss = backlst.join(' ');\n  var submitcss = submitlst.join(' ');\n  var embeds = pubfrm.embeds||{};\n%>\n<% \n  if(!fac) {\n    fac = {};\n  }\n  const rtlLangs = {\n    'ar': 'Arabic',\n    'he': 'Hebrew',\n    'fa': 'Persian',\n    'ur': 'Urdu',\n  }\n  var rtlLangText = fac.langtext || {};\n  var rtlLang = rtlLangText.language || '';\n  var facadeSetting = fac.setting || {};\n  var cl = facadeSetting.language || rtlLang || 'en';\n  var dir = cl in rtlLangs ? 'rtl' : 'ltr';\n%>\n<%\n  var faclosed = enhance.closed=='on';\n  \n  if(faclosed)\n  {\n    var urlparams = new URLSearchParams(window.location.search);\n    if(urlparams.get('ff-mode')=='preview' || urlparams.get('formbuilder')=='true') faclosed = false;\n  }\n  if(isEditMode()==false && faclosed)\n  {\n    frm = {title:frm.title, errorMessage: enhance.closedmsg||'This form is temporarily closed.'};\n    result = {code:-1};\n  }\n  if(frm.errorMessage){\n%>\n  <div class=\"ff-form ff-layout-default ff-form-error\">\n    <div class=\"ff-section\">\n      <% if(frm.title){ %>\n      <h3 class=\"ff-title\"><%-frm.title%></h3>\n      <% } %>\n      <div class=\"ff-description ff-form-closed\">\n        <p><%-frm.errorMessage%></p>\n\n        <% if(faclosed && !config.plan) { %>\n          <div class=\"ff-watermark\" style=\"display: flex !important;\">\n            <p><%-lang('')%> <%=fac.hipaache?'Formesign':(fac.neartail||fac.whatsapp)?'Neartail':''%>.</p>\n                  </div>\n        <% } %>\n      </div>\n    </div>\n  </div>\n  <br/>\n<%\n  } else if(!frm.items){\n%>\n  <div class=\"ff-alert ff-message\">\n    This form is not publicly visible. It requires Google signin to submit form (or to upload files).\n    <a href=\"https://formfacade.com/website/form-not-publicly-visible-fix.html\" target=\"_blank\">\n    Learn how to disable login to get it working</a>.\n    Or, write to formfacade@guesswork.co if you need help.\n  </div>\n  <br/>\n<% } else if(data.scraped.needsLogin==1){ %>\n  <div class=\"ff-alert ff-message\">\n    This form requires Google signin to submit form. So, it will show Google Form's page on submission.\n    Disable login for seamless user experience.\n    <a href=\"https://formfacade.com/website/formfacade-redirects-to-google-forms-onsubmit-fix.html\" target=\"_blank\">Read more</a>.\n  </div>\n  <br/>\n<% } else if((data.scraped.emailAddress==1&&data.scraped.appendEmail>0) || (data.scraped.emailAddress==3&&data.scraped.appendEmail>0)){ %>\n  <div class=\"ff-alert ff-message\">\n    You have enabled <b>Response receipts</b>. Go to <b>Settings</b> > <b>General</b> > <b>Collect email addresses</b> > Disable <b>Response receipts</b>\n    (<a href=\"https://formfacade.com/website/formfacade-redirects-to-google-forms-onsubmit-fix.html\" target=\"_blank\">Read more</a>).\n    Install \n    <a href=\"https://workspace.google.com/marketplace/app/personalizeemail/462785182165\" target=\"_blank\">this addon</a>\n    instead.\n  </div>\n  <br/>\n<% } else if(data.scraped.verifiedEmail==2){ %>\n  <div class=\"ff-alert ff-message\">\n    This form requires Google  Sign-In to submit form. So, it will show error on submission.\n    Disable collect email addresses for seamless user experience.\n  </div>\n  <br/>\n<% } else if(flagFormPhishing){ %>\n  <div class=\"ff-alert ff-message\">\n    This form has been flagged as unsafe and may be part of a phishing attack. \n    If you believe this is a mistake, please contact us at\n    <% if(fac.hipaache){ %>\n      support@formesign.com\n    <% } else if(fac.neartail||fac.whatsapp){ %>\n      support@neartail.com\n    <% } else{ %>\n      support@formfacade.com\n    <% } %>.\n  </div>\n  <br/>\n<% } else if(flag=='phishing'){ %>\n  <div class=\"ff-alert ff-message\">\n    This form has been flagged as unsafe for asking <%-hasPassword?'password':'credit card details'%>. \n    If you believe this is a mistake, please contact us at\n    <% if(fac.hipaache){ %>\n      support@formesign.com\n    <% } else if(fac.neartail||fac.whatsapp){ %>\n      support@neartail.com\n    <% } else{ %>\n      support@formfacade.com\n    <% } %>.\n  </div>\n  <br/>\n<% } else if(result && result.code==200){ %>\n  <div class=\"ff-form ff-layout-<%-layout%> ff-<%-isEditMode()?'edit':'public'%>-mode\">\n    <div class=\"ff-section\">\n      <div class=\"ff-description\">\n      <% if(draft.waphone){ %>\n        <div id=\"ff-success\" class=\"ff-success\">\n          <%-lang('Press send on WhatsApp to confirm your response.')%>\n        </div>\n        <div id=\"ff-success-hide\" class=\"ff-success\" style=\"display:none;\">\n          <% if(result.messageMark){ %>\n            <%-computeField(result.messageMark)%>\n          <% } else if(result.messagePlain){ %>\n            <%-html(computeField(result.messagePlain))%>\n          <% } else{ %>\n            <%-html(data.scraped.message?computeField(data.scraped.message):lang(''))%>\n          <% } %>\n        </div>\n      <% } else if(result.messageMark){ %>\n        <div class=\"ff-success\">\n          <%-computeField(result.messageMark)%>\n        </div>\n      <% } else if(result.messagePlain){ %>\n        <div id=\"ff-success\" class=\"ff-success\">\n          <%-html(computeField(result.messagePlain))%>\n        </div>\n      <% } else{ %>\n        <div id=\"ff-success\" class=\"ff-success\">\n          <%-html(data.scraped.message?computeField(data.scraped.message):lang('Thanks for contacting us, we will get back to you as soon as possible!'))%>\n        </div>\n      <% } %>\n      <% if(!config.plan){ %>\n        <div class=\"ff-watermark\" style=\"display: flex !important;\">\n                          </div>\n      <% } %>\n      </div>\n    </div>\n  </div>\n<% } else if(result){ %>\n  <div class=\"ff-alert ff-message\">\n    <%\n      var msg;\n      if(result.code==401)\n          msg = result.message+'. This form requires Google login. Please make it available to anonymous users.';\n      else\n          msg = result.message+'. Please fill the details correctly.';\n    %>\n    <%-msg%>\n  </div>\n  <br/>\n<% \n  } else if(draft.submitSeq>0){ \n    var mins = (new Date().getTime()-draft.submittedAt)/(1000*60);\n    mins = Math.round(mins);\n    var hrs = Math.round(mins/60);\n    var days = Math.round(hrs/24);\n    var ago = days>1?days:(hrs>1?hrs:mins);\n    var duration = days>1?'days':(hrs>1?'hours':'minutes');\n%>\n  <div class=\"ff-partial ff-message\">\n    <span><%-lang('This was submitted $ago $duration ago. You are editing #$id.', {ago, duration, id:draft.submitSeq})%></span>\n  </div>\n<% } else if(enhance.closed=='on'){ %>\n  <div class=\"ff-partial ff-message\">\n    <span><%-lang('This form is closed and is visible only in <b>PREVIEW</b> tab.')%></span>\n  </div>\n<% } else if(draft.ago && showago){ %>\n  <div class=\"ff-partial ff-message\">\n    <span><%-lang('You partially filled this form $ago minutes ago', {ago:draft.ago})%></span>\n    <span>\n      <a href=\"javascript:void(0)\" \n        onclick=\"formFacade.showago=false; formFacade.render();\"><%-lang('Continue')%></a>\n      <a href=\"javascript:void(0)\"\n        onclick=\"formFacade.showago=false; formFacade.prefill(); formFacade.render();\"><%-lang('Start over')%></a>\n    </span>\n  </div>\n<% } %>\n\n<%\n  if(fac.mapping)\n  {\n    var autocmpls = {name:'name', email:'email', address:'street-address', phone:'tel'};\n    for(var attr in fac.mapping)\n    {\n      var iid = fac.mapping[attr];\n      var itm = frmitms[iid];\n      var autonm = autocmpls[attr];\n      if(autonm && itm)\n        itm.autocomplete = autonm;\n    }\n  }\n  if(!result || result.code>200){\n%>\n  <form dir=\"<%=dir%>\" id=\"Publish<%-params.publishId%>\" \n  class=\"ff-form ff-layout-<%-layout%> ff-<%-isEditMode()?'edit':'public'%>-mode <%=dir==='ltr'?'ff-text-left':''%>\" method=\"POST\" \n  action=\"https://docs.google.com/forms/u/1/d/e/<%-data.request.params.publishId%>/formResponse\">\n    <input type=\"hidden\" name=\"id\" value=\"<%-id%>\">\n    <input type=\"hidden\" name=\"pageHistory\" value=\"\">\n    <% if(draft.responseId){ %>\n    <input type=\"hidden\" name=\"responseId\" value=\"<%-draft.responseId%>\">\n    <% } %>\n    <% if(draft.consumerId){ %>\n    <input type=\"hidden\" name=\"consumerId\" value=\"<%-draft.consumerId%>\">\n    <% } %>\n    <% if(frm.form=='native'){ %>\n    <input type=\"hidden\" name=\"form\" value=\"native\">\n    <% } %>\n    <input type=\"hidden\" id=\"Payment<%-params.publishId%>\" name=\"paymentId\" value=\"\">\n    <input type=\"hidden\" id=\"PaymentData<%-params.publishId%>\" name=\"paymentData\" value=\"\">\n    <input type=\"hidden\" id=\"Accepted<%-params.publishId%>\" name=\"accepted\" value=\"\">\n    <input type=\"hidden\" id=\"AcceptedAt<%-params.publishId%>\" name=\"acceptedAt\" value=\"\">\n    <%if(data.moveto){%>\n      <input type=\"hidden\" id=\"moveto\" name=\"moveto\" value=\"<%-data.moveto%>\">\n    <%}%>\n    <% \n      if(data.officeuseSectionId && data.approvers)\n      {\n        var approverId = officeUseSections[data.officeuseSectionId];\n        var approver = data.approvers[approverId]||{};\n        if(approver && approver.rule && approver.rule.default)\n          approver = approver.rule.default;\n        var notesItm = data.scraped.items[approver.officeUseNotes]||{};\n        var signItm = data.scraped.items[approver.officeUseSignature]||{};\n    %>\n      <input type=\"hidden\" id=\"signWorkflowSection\" name=\"signWorkflowSection\" value=\"<%-data.officeuseSectionId%>\">\n      <input type=\"hidden\" id=\"signWorkflowApprover\" name=\"signWorkflowApprover\" value=\"<%-approver.destination%>\">\n      <input type=\"hidden\" id=\"signWorkflowNotes\" name=\"signWorkflowNotes\" value=\"<%-notesItm.entry%>\">\n      <input type=\"hidden\" id=\"signWorkflowSignature\" name=\"signWorkflowSignature\" value=\"<%-signItm.entry%>\">\n    <%}%>\n    <% \n      var item;\n      sections.forEach(function(sec,s){\n    %>\n    <div class=\"ff-section\" id=\"ff-sec-<%=sec.id%>\" \n      style=\"<%-isEditMode()?'display:block':(sec.id==draft.activePage?'display:block':'display:none')%>;\">\n      <%\n        var ttls = fac.titles?fac.titles:{};\n        var ttl = ttls[sec.id]?ttls[sec.id]:{};\n        var itmnxt = fac.next?fac.next[sec.id]:null;\n        var pgbrk = frm.items?frm.items[sec.id]:null;\n        var bannerimg = sec.id=='root'?frm.bgimage:null;\n        if(ttl.banner) bannerimg = ttl.banner;\n      %>\n      <% if(bannerimg){ %>\n        <img src=\"<%-switchCDN(bannerimg)%>\" class=\"ff-banner-image ff-image\"/>\n      <% } %>\n      <h3 class=\"ff-title\" id=\"ff-title-<%-sec.id%>\">\n        <%-ttl.title?html(computeField(ttl.title)):(sec.titleMark||html(sec.title))%>\n        <% if(isEditMode()){ %>\n          <% if(s==0){ %>\n            <i class=\"ff-customize material-icons\" onclick=\"editFacade.afterLoad('showCustomize')\">settings</i>\n          <% } else if(pgbrk && pgbrk.duplicate){ %>\n            <img src=\"/img/loading_gear.svg\" class=\"ff-editsection ff-waiting\"/>\n          <% } else{ %>\n            <i class=\"ff-editsection material-icons\" onclick=\"editFacade.afterLoad('showTitle', '<%-sec.id%>')\">settings</i>\n          <% } %>\n        <% } %>\n      </h3>\n      <%\n        var desc = sec.description?sec.description:(isEditMode()?'(No description)':null);\n        if(ttl.messageMark)\n        {\n      %>\n        <div class=\"ff-description mdViewer\" id=\"ff-desc-<%-sec.id%>\">\n          <%-switchAllCDN(computeField(ttl.messageMark))%>\n        </div>\n      <%\n        } else if(desc){ \n      %>\n      <div class=\"ff-description mdViewer\">\n        <p>\n          <%-switchAllCDN(sec.helpMark||html(desc))%>\n        </p>\n      </div>\n      <% } %>\n    <div class=\"ff-secfields\">\n    <% if(s==0 && data.scraped.appendEmail==1){%>\n      <div class=\"<%-prepend('form-group')%> ff-item ff-emailAddress\">\n          <label class=\"ff-item-qs\" for=\"WidgetemailAddress\"><%-lang('Email address')%> <span class=\"ff-required\">*</span></label>\n          <input type=\"email\" pattern=\"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$\"\n            class=\"<%-prepend('form-control')%> ff-email-widget\" id=\"WidgetemailAddress\" name=\"emailAddress\" \n            value=\"<%=draft.emailAddress%>\" required>\n          <div id=\"ErroremailAddress\" class=\"ff-widget-error\"></div>\n      </div>\n    <% } %>\n    <%\n      var oitems = fac.items?fac.items:{};\n      splitHeaders(sec.headers).forEach(function(header, hdi){\n    %>\n      <% \n        if(header.head){ \n          item = header.head;\n          var ttls = fac.titles?fac.titles:{};\n          var ttl = ttls[item.id]?ttls[item.id]:{};\n          var oitem = oitems[item.id]?oitems[item.id]:{};\n      %>\n        <div class=\"<%-prepend('form-group')%> ff-item <%-oitem.mode=='hide'?(isEditMode()?'ff-edit-hide':'ff-hide'):''%> <%-item.outstock?'ff-head-outstock':''%> ff-section_header ff-full-width <%-item.hasNavigation?('ff-nav-dyn'):''%> <%-item.product?'ff-item-prd':'ff-item-noprd'%>\" id=\"ff-id-<%-item.id||item.outstock%>\">\n          <h4 class=\"ff-section-header\" id=\"ff-title-<%-item.id||item.outstock%>\">\n            <%-ttl.title?ttl.title:(item.titleMark||item.title)%>\n            <% if(isEditMode()){ %>\n              <% if(officeUseSectionIds.indexOf(item.id)>-1){ %>\n                  ðŸ’¼\n              <% } %>\n              <% if(item.duplicate){ %>\n                <img src=\"/img/loading_gear.svg\" class=\"ff-editsection ff-waiting\"/>\n              <% } else if(item.id){ %>\n                <i class=\"ff-editsection material-icons\" onclick=\"editFacade.afterLoad('showTitle', '<%-item.id%>')\">settings</i>\n              <% } %>\n            <% } %>\n          </h4>\n          <%\n            var desc = item.help?item.help:(isEditMode()?'(No description)':null);\n            if(ttl.messageMark)\n            {\n          %>\n            <div class=\"ff-description mdViewer\" id=\"ff-desc-<%-item.id%>\">\n              <%-switchAllCDN(computeField(ttl.messageMark))%>\n            </div>\n          <%\n            } else if(desc){ \n          %>\n            <div class=\"ff-description mdViewer\">\n              <p>\n                <%-switchAllCDN(item.helpMark||html(desc))%>\n              </p>\n            </div>\n          <% } %>\n        </div>\n      <% } %>\n      <%\n        if(frm.shuffle)\n        {\n          var shufitms = header.items.filter(sh=>sh.type=='SECTION_HEADER');\n          var subshuf = header.items.filter(sh=>sh.type!='SECTION_HEADER');\n          header.items = shufitms.concat(shuffle(subshuf));\n        }\n        header.items.forEach(function(itm, itmi){\n          item = itm;\n          var itmval = draft.entry[item.entry];\n          var oitem = oitems[item.id]||{};\n          if(item.product && oitem.measure === 'Nested' && oitem.nested === 'Appointment' && layout !== 'default') item.product = null;\n          var getScoreHint = function(chi)\n          {\n            var scorelst = oitem.score||[];\n            var scoreval = scorelst[chi];\n            if(isEditMode() && isNaN(scoreval)==false)\n            {\n              var emojiscore = scoreval>=0?'+':'';\n              if(item.type=='LIST')\n              {\n                return ` (${emojiscore}${scoreval})`;\n              }\n              else\n              {\n                return `<span onclick=\"editFacade.afterLoad('showWidget', '${item.id}', 'pills-answer-tab')\"\n                class=\"ff-emoji-score\" title=\"Edit points\">${emojiscore}${scoreval}</span>`;\n              }\n            }\n            return '';\n          }\n          var outstock = oitem.inventory=='yes' && oitem.remain<=0;\n          var fftype = item.type?item.type.toLowerCase():'unknown';\n          if(oitem.type=='FILE_UPLOAD') fftype = 'file';\n          var fullwidgets = ['section_header', 'paragraph_text', 'multiple_choice', 'checkbox', 'grid', 'scale', 'file', 'image', 'video'];\n          var ffdisplay = fullwidgets.indexOf(fftype)>=0?'ff-full-width':'ff-part-width';\n          var onclick;\n          if(isEditMode())\n          {\n            if((item.product && hasCreator()) || (oitem &&  oitem.measure === 'Nested'))\n              onclick = `onclick=\"editFacade.afterLoad('showProduct', '${item.id}')\"`;\n            else if (item.type == 'VIDEO')\n              onclick = `onclick=\"editFacade.afterLoad('showVideo', '${item.id}')\"`;\n            else if(item.type == 'IMAGE')\n              onclick = `onclick=\"editFacade.afterLoad('showImage', '${item.id}')\"`;\n            else\n              onclick = `onclick=\"editFacade.afterLoad('showWidget', '${item.id}')\"`;\n          }\n          else\n          {\n            if(item.product && layout!='default')\n              onclick = 'onclick=\"formFacade.showProduct('+item.id+')\"';\n            else if (oitem && oitem.measure === 'Nested' && oitem.nested === 'Appointment' && layout !== 'default') \n            onclick = 'onclick=\"formFacade.showProduct('+item.id+')\"';\n          }\n          var oncartClick = 'onclick=\"event.stopPropagation();formFacade.showProduct('+item.id+', 2)\"';\n      %>\n        <div class=\"<%-prepend('form-group')%> ff-item <%-(oitem.mode=='hide'||oitem.mode=='officeuse')?(isEditMode()?'ff-edit-hide':'ff-hide'):''%> <%-outstock?(isEditMode()?'ff-edit-outstock':'ff-outstock'):''%> ff-<%-fftype%> <%-ffdisplay%> <%-item.hasNavigation?('ff-nav-dyn'):''%> <%-item.product?'ff-item-prd':'ff-item-noprd'%>\" id=\"ff-id-<%-item.id%>\" <%-onclick%>>\n          <% \n            if(!oitem.mode || oitem.mode=='edit' || oitem.mode=='read' || isEditMode()){ \n              var qsttl = oitem.title?html(computeField(oitem.title)):(item.titleMark||html(item.title));\n              var isqshtml = /<\\/?[a-z][\\s\\S]*>/i.test(qsttl);\n          %>\n            <label <%-onclick%> class=\"ff-item-qs <%-qsttl?(isqshtml?'ff-qs-html':''):'ff-qs-empty'%>\" for=\"Widget<%-item.id%>\">\n                <% if(isqshtml){ %>\n                  <div class=\"ff-qs-html-text\">\n                    <%-qsttl%>\n                  </div>\n                <% } else{ %>\n                  <%-qsttl%>\n                <% } %>\n                <% if(item.required){ %><span class=\"ff-required\">*</span> <% } %>\n                <% if(oitem.tag && isEditMode()){ %>\n                  <span onclick=\"editFacade.afterLoad('showWidget', '<%-item.id%>', 'pills-answer-tab')\"\n                  class=\"ff-emoji-score\" title=\"Edit category\">ðŸ·ï¸ <%-oitem.tag%></span>\n                <% } %>\n                <% if(oitem.mode=='officeuse' && isEditMode()){ %>\n                  ðŸ’¼\n                <% } %>\n                <% if(oitem.encrypt=='PHI' && isEditMode()){ %>\n                  ðŸ”’\n                <% } %>\n                <% if(item.duplicate && hasCreatorOrEditor()){ %>\n                  <img src=\"/img/loading_gear.svg\" class=\"ff-editwidget ff-waiting\"/>\n                <% } else if(item.product && hasCreator()){ %>\n                  <i <%-onclick%> class=\"ff-editwidget material-icons\">\n                    settings\n                  </i>\n                <% } else{ %>\n                  <i <%-onclick%> class=\"ff-editwidget material-icons\">\n                    settings\n                  </i>\n                <% } %>\n            </label>\n            <% if(oitem.price>=0){ %>\n              <small <%-onclick%> id=\"Price<%-item.id%>\" class=\"ff-price ff-help form-text\">\n                <% if(item.price && item.price.fullformat){ %>\t\n                  <s><%-item.price.fullformat%></s>\t\n                <% } %>\n                <% if(oitem.price==0 && item.price && item.price.minformat){ %>\n                  <%-lang('From $minprice', {minprice:item.price.minformat})%>\n                <% } else{ %>\n                  <%-oitem.helpMark%>\n                <% } %>\n                <% if(oitem.measure=='Weight'){ %>\n                  <%-lang('per')%> <%-unit%>\n                <% } %>\n              </small>\n            <% } else if(oitem.helpMark){ %>\n              <% if(oitem.helpMark.indexOf('${')>=0){ %>\n                <small <%-onclick%> id=\"Help<%-item.id%>\" class=\"ff-help form-text mdViewer\">\n                  <%-switchAllCDN(computeField(oitem.helpMark, item))%>\n                </small>\n              <% } else{ %>\n                <small <%-onclick%> id=\"Static<%-item.id%>\" class=\"ff-help form-text mdViewer\">\n                  <%-switchAllCDN(oitem.helpMark)%>\n                </small>\n              <% } %>\n            <% } else if(item.help){ %>\n              <small <%-onclick%> id=\"Static<%-item.id%>\" class=\"ff-help form-text mdViewer\"><%-switchAllCDN(item.helpMark||item.help)%></small>\n            <% } else{ %>\n              <small <%-onclick%> id=\"Static<%-item.id%>\" class=\"ff-help-empty ff-help form-text mdViewer\"></small>\n            <% } %>\n            <% if(oitem.prddetailMD){ %>\n              <div <%-onclick%> id=\"Detail<%-item.id%>\" class=\"ff-detail form-text mdViewer\">\n                <%-switchAllCDN(oitem.prddetailMark)%>\n              </div>\n            <% } %>\n            <% \n              var ttlimg;\n              if(oitem.prdimage)\n                ttlimg = switchCDN(oitem.prdimage);\n              else if(item.titleImage){\n                ttlimg = 'https://formfacade.com/itemimg/'+params.publishId+'/item/'+item.id+'/title/'+item.titleImage.blob;\n                if(item.titleImage.image) {\n                  ttlimg = `https://formfacade.com/itemload/item/${encode(item.titleImage.image)}`;\n                }\n              } else if(item.product)\n                ttlimg = item.duplicate?'/img/waiting.svg':'https://formfacade.com/img/image-not-found.png';\n              if(ttlimg){ \n            %>\n              <img src=\"<%-ttlimg%>\" alt=\"<%=s>0&&isEditMode()?'Use preview to see this image':item.title%>\"\n                <% if(item.product){ %>\n                  <%-onclick%>\n                <% } else if(item.titleImage && item.titleImage.size){ %>\n                  style=\"width:<%-item.titleImage.size.width%>px;  \n                  <% if(item.titleImage.size.align){ %> \n                    margin-left:<%-item.titleImage.size.align==0?'0px':'auto'%>; margin-right:<%-item.titleImage.size.align==2?'0px':'auto'%>;\n                  <% } %>\n                  max-width:100%; height:auto;\"\n                <% } %>\n                <% if(isEditMode() || sec.id!=draft.activePage){ %>\n                  loading=\"lazy\"\n                <% } %>\n                class=\"ff-title-image ff-image <%-oitem.prdimage||item.titleImage?'ff-image-found':'ff-image-not-found'%>\"/>\n            <% } %>\n          <% } %>\n          <% if(oitem.mode=='hide' && item.entry && isEditMode()==false){ %>\n            <input type=\"hidden\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\" value=\"<%=itmval%>\">\n          <% } else if(oitem.mode=='read' || oitem.calculated){ %>\n            <% if(item.type=='PARAGRAPH_TEXT'){ %>\n              <textarea class=\"ff-widget-control ff-readonly <%-prepend('form-control')%>\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\"\n                rows=\"3\" readonly><%-itmval%></textarea>\n            <% } else if(item.type=='DATE'){ %>\n              <input type=\"text\" class=\"ff-widget-control ff-readonly <%-prepend('form-control')%>\" id=\"Display<%-item.id%>\" value=\"<%=itmval%>\" readonly <%-item.required?'required':''%>>\n              <input type=\"hidden\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\" value=\"<%=itmval%>\">\n            <% } else if(oitem.subtype=='SIGNATURE') {%>\n              <div class=\"ff-widget-control\" style=\"margin-top:5px;\">\n                <% if(itmval) { %>\n                  <img src=\"<%-itmval%>\" />\n                <% } %>\n              </div>\n              <input type=\"hidden\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\" value=\"<%=itmval%>\">\n            <% } else if(item.type=='GRID') { %>\n              <% \n                var chs = filter(item.choices)\n                item.rows.forEach(function(rw, rwi){ \n                  var rvals = draft.entry[rw.entry];\n                  rvals = Array.isArray(rvals)?rvals:[rvals];\n              %>\n                <input type=\"text\" class=\"ff-widget-control ff-readonly <%-prepend('form-control')%>\" id=\"Display<%-item.id%>\" value=\"<%=rw.value + \": \" + rvals%>\" readonly <%-item.required?'required':''%>>\n                <% rvals.forEach(function(val){ %>\n                  <input type=\"hidden\"  name=\"entry.<%-rw.entry%>\" id=\"entry.<%-rw.entry%>.<%=val%>\" value=\"<%=val%>\">\n                <% }) %>\n              <% }) %>\n            <% } else {%>\n              <input type=\"text\" class=\"ff-widget-control ff-readonly <%-prepend('form-control')%>\" id=\"Display<%-item.id%>\" value=\"<%=itmval%>\" readonly <%-item.required?'required':''%>>\n              <% if(Array.isArray(itmval)) {%>\n                <% itmval.forEach(function(val){ %>\n                  <input type=\"hidden\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\" value=\"<%=val%>\">\n                <% }) %>\n              <% } else { %>\n                <input type=\"hidden\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\" value=\"<%=itmval%>\">\n              <% } %>\n            <% } %>\n          <% } else if(oitem.type=='FILE_UPLOAD' && oitem.subtype=='SIGNATURE'){ %>\n            <input type=\"hidden\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\" value=\"<%=itmval%>\">\n            <div id=\"Display<%-item.id%>\" class=\"ff-widget-control ff-signature\" data-entry=\"<%-item.entry%>\" data-id=\"<%-item.id%>\">\n              <canvas id=\"esign_<%-item.id%>\" class=\"esign_canvas\" width=\"450\" height=\"200\"></canvas>\n              <hr>\n              <span class=\"sign_clear\" onclick=\"formFacade.clearSignature('<%-item.id%>','<%-item.entry%>')\">Clear</span>\n            </div>\n          <% } else if(oitem.type=='FILE_UPLOAD'){ %>\n            <input type=\"hidden\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\" value=\"<%=itmval%>\">\n            <div id=\"Display<%-item.id%>\" class=\"ff-widget-control ff-file-upload\" \n              data-files=\"<%=itmval%>\" data-entry=\"<%-item.entry%>\" data-id=\"<%-item.id%>\">\n            </div>\n          <% } else if(item.type=='TEXT'){ %>\n            <% if(item.validOperator=='Email'){ %>\n              <input type=\"email\" pattern=\"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$\"\n                class=\"ff-widget-control <%-prepend('form-control')%> ff-email-widget\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\"\n                <% if(item.autocomplete=='email'){ %> \n                  autocomplete=\"<%-item.autocomplete%>\" autocorrect=\"off\" spellcheck=\"false\"\n                <% } %>\n            <% } else{ %>\n              <input type=\"text\" class=\"ff-widget-control <%-prepend('form-control')%>\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\"\n                <% if(item.autocomplete){ %> \n                  autocomplete=\"<%-item.autocomplete%>\" autocorrect=\"off\" spellcheck=\"false\"\n                <% } %>\n            <% } %>\n            <% if(item.autocomplete=='email' && fac.hipaache && fac.mapping && fac.mapping.score){ %>\n              onchange=\"formFacade.getHistory(this.value)\"\n            <% } %>\n            value=\"<%=itmval%>\" placeholder=\"<%=oitem.placeholder%>\" <%-item.required?'required':''%>>\n          <% } else if(item.type === 'PARAGRAPH_TEXT' && oitem && oitem.measure === 'Nested' && oitem.nested === 'Appointment' && layout != 'default') {\n            var dsText = null;\n            if(itmval && typeof itmval == 'string' && itmval.indexOf(' || ') > 0)\n            {\n              var [vs, v1WithQty] = itmval.split(' || ');\n              var [v1s, q] = v1WithQty.split(' * ');\n              var v1 = v1s.split(' | ')[0];\n              var v = vs.split(' | ')[0];\n              if(q.trim() =='1') dsText = `${v} ${v1}`;\n            }\n          %>\n            <textarea class=\"ff-widget-control <%-prepend('form-control')%>\" style=\"display:none\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\" placeholder=\"<%=oitem.placeholder%>\" <%-item.required?'required':''%> rows=\"3\"><%-itmval%></textarea>\n            <div tabindex=\"0\" class=\"ff-full-width ff-appointment-container <%=dsText ? 'ff-appointment-selected' : ''%>\" onclick=\"<%=onclick%>\">\n              <% if(dsText) { %>\n                <div class='ff-appointment-calendar-container'>\n                  <div class='ff-appointment-calendar'>\n                    <p class=\"ff-appointment-calendar-month\">\n                      <%-getMonth(dsText)%>\n                    </p>\n                    <b class=\"ff-appointment-calendar-date\">\n                      <%-getDate(dsText)%>\n                    </b>\n                    <p class=\"ff-appointment-calendar-day\">\n                      <%-getDay(dsText)%>\n                    </p>\n                  </div>\n                </div>\n                <div class=\"ff-appointment-text-container\">\n                  <p class=\"ff-appointment-text ff-appointemt-selected\">\n                    Selected Time\n                  </p>\n                  <b class=\"ff-appointment-time\">\n                    <%=getFormattedDate(item.id, dsText)%>\n                  </b>\n                  <b class=\"ff-appointment-time\">\n                    <%=getFormattedTime(item.id, dsText)%>\n                  </b>\n                </div>\n              <% } else { %>\n                <div class=\"ff-appointment-placeholder-container\">\n                  <span class=\"material-icons\">\n                    calendar_month\n                  </span>\n                  <div class=\"ff-appointment-placeholder-text\">\n                    <%-lang('Select a time')%>\n                  </div>\n                </div>\n              <% } %>\n            </div>\n          <% } else if(item.type=='PARAGRAPH_TEXT'){ %>\n            <textarea class=\"ff-widget-control <%-prepend('form-control')%>\" id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\"\n              <% if(item.autocomplete){ %> \n                autocomplete=\"<%-item.autocomplete%>\" autocorrect=\"off\" spellcheck=\"false\"\n              <% } %>\n              placeholder=\"<%=oitem.placeholder%>\" <%-item.required?'required':''%> rows=\"3\"><%-itmval%></textarea>\n          <% } else if(item.type=='LIST'){ %>\n            <% var chs = item.choices?item.choices:[] %>\n            <% if(chs.length<=200){ %>\n              <select class=\"ff-widget-control <%-prepend('form-control')%>\" id=\"Widget<%-item.id%>\" \n                name=\"entry.<%-item.entry%>\" <%-item.required?'required':''%>>\n              <option value=\"\">- <%-oitem.placeholder?oitem.placeholder:lang('Choose')%> -</option>\n              <% chs.forEach(function(ch, chi){ %>\n                <option <%-itmval==ch.value?'selected':''%> value=\"<%=ch.value%>\"><%-ch.value%><%-getScoreHint(chi)%></option>\n              <% }) %>\n              </select>\n            <% } else { %>\n              <input type=\"text\" class=\"ff-widget-control <%-prepend('form-control')%> ff-long-list\" \n                id=\"Widget<%-item.id%>\" name=\"entry.<%-item.entry%>\"\n                value=\"<%=itmval%>\" <%-item.required?'required':''%> list=\"List<%-item.id%>\" autocomplete=\"off\"\n                onkeypress=\"return event.keyCode!=13;\">\n              <datalist id=\"List<%-item.id%>\" class=\"ff-datalist\">\n              <% chs.forEach(function(ch){ %>\n                <option><%-ch.value%></option>\n              <% }) %>\n              </datalist>\n            <% } %>\n          <% } else if(item.type=='CHECKBOX'){ %>\n            <% \n              var chs = filter(item.choices);\n              if(item.shuffle)\n              {\n                var lst = chs[chs.length-1].value?null:chs.pop();\n                chs = shuffle(chs);\n                if(lst) chs.push(lst);\n              }\n              var chsels = itmval?(Array.isArray(itmval)?itmval:[itmval]):[];\n              var chimgblobs = chs.filter(function(ch){ return ch.blob });\n              var chimgs = chs.filter(function(ch){ return ch.image });\n            %>\n            <% if(chimgblobs.length>0 || chimgs.length > 0){ %> \n              <div class=\"ff-widget-control ff-check-table ff-wrap-images\">\n                <% chs.forEach(function(ch, chi){ %>\n                  <% \n                      if(ch.value)\n                      {\n                        var imgSrc; var imgTempStyle;\n                        if(ch.image) {\n                          imgSrc = `https://formfacade.com/itemload/item/${encode(ch.image+'=w260')}`; // load google image\n                          if(ch.image.indexOf('neartail.com')>0){\n                            imgSrc = ch.image;\n                            imgTempStyle = 'max-width: 260px;'\n                          }\n                        } else if(chimgblobs[chi]) {\n                          imgSrc = `https://formfacade.com/itemimg/${params.publishId}/item/${item.id}/choice/${chimgblobs[chi].blob}`;\n                        }\n                    %>\n                    <div onclick=\"let input = this.querySelector('input');if(input) { input.click() }\" class=\"ff-form-check ff-check-cell\">\n                      <% if(imgSrc){ %>\n                      <img class=\"ff-check-cell-image\"\n                        alt=\"<%-s>0&&isEditMode()?'Use preview to see this image':''%>\"\n                        style=\"<%-imgTempStyle?imgTempStyle:''%>\"\n                        src=\"<%-imgSrc%>\"/>\n                      <% } %>\n                      <input class=\"ff-form-check-input ff-pointer-events-none\" type=\"checkbox\" name=\"entry.<%-item.entry%>\" id=\"entry.<%-item.entry%>.<%=ch.value%>\" \n                        <%-chsels.indexOf(ch.value)>=0?'checked':''%> value=\"<%=ch.value%>\">\n                      <label class=\"ff-form-check-label ff-pointer-events-none\" for=\"entry.<%-item.entry%>.<%=ch.value%>\">\n                        <%-ch.value%><%-getScoreHint(chi)%>\n                      </label>\n                    </div>\n                  <% } else{ %>\n                    <div onclick=\"document.getElementById('entry.<%-item.entry%>.other_option_response').click()\" class=\"ff-form-check ff-form-check-other ff-pointer-cursor\">\n                      <input class=\"ff-form-check-input ff-pointer-disable\" type=\"checkbox\" <%=draft.entry[item.entry+'-other_option_response']?'checked':''%>\n                        name=\"entry.<%-item.entry%>\" id=\"entry.<%-item.entry%>.other_option_response\" value=\"__other_option__\">\n                      <input class=\"<%-prepend('form-control')%>\" type=\"text\" name=\"entry.<%-item.entry%>.other_option_response\"\n                        value=\"<%=draft.entry[item.entry+'-other_option_response']%>\" placeholder=\"<%=oitem.placeholder?oitem.placeholder:'Other'%>\"\n                        oninput=\"document.getElementById(this.name).checked=true\"/>\n                    </div>\n                  <% } %>\n                <% }) %>\n              </div>\n            <% } else{ %>\n              <div class=\"ff-widget-control ff-check-table <%-item.wrap?'ff-wrap-choices':''%>\">\n              <% chs.forEach(function(ch, chi){ %>\n                <% if(ch.value){%>\n                <div class=\"ff-form-check\">\n                  <input class=\"ff-form-check-input\" type=\"checkbox\" name=\"entry.<%-item.entry%>\" id=\"entry.<%-item.entry%>.<%=ch.value%>\" \n                    <%-chsels.indexOf(ch.value)>=0?'checked':''%> value=\"<%=ch.value%>\">\n                  <label class=\"ff-form-check-label\" for=\"entry.<%-item.entry%>.<%=ch.value%>\">\n                    <%-ch.value%><%-getScoreHint(chi)%>\n                  </label>\n                </div>\n                <% } else{ %>\n                  <div onclick=\"document.getElementById('entry.<%-item.entry%>.other_option_response').click()\" class=\"ff-form-check ff-form-check-other ff-pointer-cursor\">\n                    <input class=\"ff-form-check-input ff-pointer-disable\" type=\"checkbox\" <%=draft.entry[item.entry+'-other_option_response']?'checked':''%>\n                      name=\"entry.<%-item.entry%>\" id=\"entry.<%-item.entry%>.other_option_response\" value=\"__other_option__\">\n                    <input class=\"<%-prepend('form-control')%>\" type=\"text\" name=\"entry.<%-item.entry%>.other_option_response\"\n                      value=\"<%=draft.entry[item.entry+'-other_option_response']%>\" placeholder=\"<%=oitem.placeholder?oitem.placeholder:'Other'%>\"\n                      oninput=\"document.getElementById(this.name).checked=true\"/>\n                  </div>\n                <% } %>\n              <% }) %>\n              </div>\n            <% } %>\n            <input type=\"hidden\" name=\"entry.<%-item.entry%>_sentinel\" title=\"<%=item.title%>\" class=\"<%-item.required?'ff-check-required':''%>\"/>\n          <% } else if(item.type=='MULTIPLE_CHOICE'){ %>\n            <% \n              var chs = filter(item.choices);\n              if(item.shuffle)\n              {\n                var lst = chs[chs.length-1].value?null:chs.pop();\n                chs = shuffle(chs);\n                if(lst) chs.push(lst);\n              }\n              var chsels = itmval?(Array.isArray(itmval)?itmval:[itmval]):[];\n              var chimgblobs = chs.filter(function(ch){ return ch.blob });\n              var chimgs = chs.filter(function(ch){ return ch.image });\n            %>\n            <% if(chimgblobs.length>0 || chimgs.length > 0){ %> \n              <div class=\"ff-widget-control ff-check-table ff-wrap-images\">\n                <% chs.forEach(function(ch, chi){ %>\n                  <% \n                    if(ch.value)\n                    {\n                      var imgSrc;\n                      if(ch.image) {\n                        imgSrc = `https://formfacade.com/itemload/item/${encode(ch.image+'=w260')}`; // load google image\n                        if(ch.image.indexOf('neartail.com')>0)\n                          imgSrc = ch.image;\n                      } else if(chimgblobs[chi]) {\n                        imgSrc = `https://formfacade.com/itemimg/${params.publishId}/item/${item.id}/choice/${chimgblobs[chi].blob}`;\n                      }\n                  %>\n                    <div onclick=\"let input = this.querySelector('input');if(input) { input.click() }\" class=\"ff-form-check ff-check-cell\">\n                      <% if(imgSrc){ %>\n                        <img class=\"ff-check-cell-image\" \n                          alt=\"<%-s>0&&isEditMode()?'Use preview to see this image':''%>\"\n                          src=\"<%-imgSrc%>\"\n                        />\n                      <% } %>\n                      <input class=\"ff-form-check-input ff-pointer-events-none\" type=\"radio\" name=\"entry.<%-item.entry%>\" id=\"entry.<%-item.entry%>.<%=ch.value%>\" \n                        onclick=\"entr=<%-item.entry%>; if(formFacade.draft.entry[entr]==this.value){ delete formFacade.draft.entry[entr]; this.checked=false; formFacade.saveDraft(); }\"\n                        <%-chsels.indexOf(ch.value)>=0?'checked':''%> value=\"<%=ch.value%>\" <%-item.required?'required':''%>>\n                      <label class=\"ff-form-check-label ff-pointer-events-none\" for=\"entry.<%-item.entry%>.<%=ch.value%>\">\n                        <%-ch.value%><%-getScoreHint(chi)%>\n                      </label>\n                    </div>\n                  <% } else{ %>\n                    <div onclick=\"document.getElementById('entry.<%-item.entry%>.other_option_response').click()\" class=\"ff-form-check ff-form-check-other ff-pointer-cursor\">\n                      <input class=\"ff-form-check-input ff-pointer-disable\" type=\"radio\" <%=draft.entry[item.entry+'-other_option_response']?'checked':''%>\n                        onclick=\"entr=<%-item.entry%>; if(formFacade.draft.entry[entr]==this.value){ delete formFacade.draft.entry[entr]; this.checked=false; formFacade.saveDraft(); }\"\n                        name=\"entry.<%-item.entry%>\" id=\"entry.<%-item.entry%>.other_option_response\" value=\"__other_option__\">\n                      <input class=\"<%-prepend('form-control')%>\" type=\"text\" name=\"entry.<%-item.entry%>.other_option_response\"\n                        value=\"<%=draft.entry[item.entry+'-other_option_response']%>\" placeholder=\"<%=oitem.placeholder?oitem.placeholder:'Other'%>\"\n                        oninput=\"document.getElementById(this.name).checked=true\"/>\n                    </div>\n                  <% } %>\n                <% }) %>\n              </div>\n            <% } else{ %>\n              <div class=\"ff-widget-control ff-check-table <%-item.wrap?'ff-wrap-choices':''%>\">\n              <% chs.forEach(function(ch, chi){ %>\n                <% if(ch.value){ %>\n                  <div class=\"ff-form-check\">\n                    <input class=\"ff-form-check-input\" type=\"radio\" name=\"entry.<%-item.entry%>\" id=\"entry.<%-item.entry%>.<%=ch.value%>\" \n                      onclick=\"entr=<%-item.entry%>; if(formFacade.draft.entry[entr]==this.value){ delete formFacade.draft.entry[entr]; this.checked=false; formFacade.saveDraft(); }\"\n                      <%-chsels.indexOf(ch.value)>=0?'checked':''%> value=\"<%=ch.value%>\" <%-item.required?'required':''%>>\n                    <label class=\"ff-form-check-label\" for=\"entry.<%-item.entry%>.<%=ch.value%>\">\n                      <%-ch.value%><%-getScoreHint(chi)%>\n                    </label>\n                  </div>\n                <% } else{ %>\n                  <div onclick=\"document.getElementById('entry.<%-item.entry%>.other_option_response').click()\" class=\"ff-form-check ff-form-check-other ff-pointer-cursor\">\n                    <input class=\"ff-form-check-input ff-pointer-disable\" type=\"radio\" <%=draft.entry[item.entry+'-other_option_response']?'checked':''%>\n                        onclick=\"entr=<%-item.entry%>; if(formFacade.draft.entry[entr]==this.value){ delete formFacade.draft.entry[entr]; this.checked=false; formFacade.saveDraft(); }\"\n                        name=\"entry.<%-item.entry%>\" id=\"entry.<%-item.entry%>.other_option_response\" value=\"__other_option__\">\n                    <input class=\"<%-prepend('form-control')%>\" type=\"text\" name=\"entry.<%-item.entry%>.other_option_response\"\n                      value=\"<%=draft.entry[item.entry+'-other_option_response']%>\" placeholder=\"<%=oitem.placeholder?oitem.placeholder:'Other'%>\"\n                      oninput=\"document.getElementById(this.name).checked=true\"/>\n                  </div>\n                <% } %>\n              <% }) %>\n              </div>\n            <% } %>\n          <% } else if(item.type=='SCALE'){ %>\n            <!-- LINEAR SCALE -->\n          <% var chs = filter(item.choices) %>\n          <% if (layout !== 'default') { %>\n          <!-- FOR 1COLUMN and 2COLUMN -->\n          <div class=\"ff-linear-scale-container\">\n            <div class=\"ff-linear-scale-inner-container\">\n              <% if(isEditMode()) { %>\n                <div class=\"ff-widget-control ff-linear-scale-wrapper\" style=\"margin: 0px;margin-bottom:-.75em; padding: 0px;padding-top: 10px;\">\n                  <% chs.forEach(function(ch, chi){ %>\n                    <% if (getScoreHint(chi)){ %>\n                    <div class=\"ff-linear-scale-input-wrapper\">\n                      <div class=\"ff-linear-scale-button ff-linear-scale-disabled <%=chi===0 ? 'ff-linear-scale-button-first' : chi===chs.length - 1 ? 'ff-linear-scale-button-last' : ''%>\" style=\"border:transparent;\"><%-getScoreHint(chi)%></div>\n                    </div>\n                    <% } %>\n                  <% }) %>\n                </div>\n              <% } %>\n              <div class=\"ff-widget-control ff-linear-scale-wrapper\">\n              <% chs.forEach(function(ch, chi){ %>\n                <div class=\"ff-linear-scale-input-wrapper\">\n                  <input class=\"ff-linear-scale-input\" type=\"radio\" name=\"entry.<%-item.entry%>\" \n                  <%-item.required?'required':''%> <%-itmval==ch.value?'checked':''%> \n                  id=\"entry.<%-item.entry%>.<%=ch.value%>\" value=\"<%=ch.value%>\"/>\n                  <button \n                    type=\"button\"\n                    id=\"entry.<%-item.entry%>.<%-ch.value%>.button\"\n                    linear-<%-item.entry%>\n                    value=\"<%-ch.value%>\"\n                    onclick=\"\n                      var entr=<%-item.entry%>;\n                      if(formFacade.draft.entry[entr]==this.value && <%-item.required ? false : true%>) {\n                        delete formFacade.draft.entry[entr];\n                        document.getElementById('entry.<%-item.entry%>.<%=ch.value%>').checked = false;\n                        formFacade.saveDraft();\n                        document.querySelectorAll('[linear-<%-item.entry%>]').forEach(function(el) { \n                          el.classList.remove('ff-linear-scale-button-active');\n                          el.classList.remove('ff-linear-scale-button-partial-active');\n                        });\n                        return;\n                      };\n                      document.querySelectorAll('[linear-<%-item.entry%>]')\n                        .forEach(function(el) { \n                          if(el.id=='entry.<%-item.entry%>.<%-ch.value%>.button') {\n                            el.classList.add('ff-linear-scale-button-active');\n                            el.classList.remove('ff-linear-scale-button-partial-active');\n                            document.getElementById('entry.<%-item.entry%>.<%=ch.value%>').click();\n                          }\n                          else if (Number(<%-ch.value%>)>Number(el.value)) {\n                            el.classList.remove('ff-linear-scale-button-active');\n                            el.classList.add('ff-linear-scale-button-partial-active');\n                          }\n                          else {\n                            el.classList.remove('ff-linear-scale-button-active');\n                            el.classList.remove('ff-linear-scale-button-partial-active');\n                          }\n                        });\n                    \"\n                    data-active-scale=\"<%-ch.value === itmval ? 'active' : ''%>\"\n                    class=\"ff-linear-scale-button <%=chi===0 ? 'ff-linear-scale-button-first' : chi===chs.length - 1 ? 'ff-linear-scale-button-last' : ''%> <%-Number(itmval)==Number(ch.value)? 'ff-linear-scale-button-active' : Number(itmval)>Number(ch.value) ? 'ff-linear-scale-button-partial-active' : ''%>\"\n                  >\n                    <%-ch.value%>\n                  </button>\n                </div>\n              <% }) %>\n              </div>\n              <% \n                let widthFor11Boxes = 150;\n                let widthForLabel = \"min-content\";\n                let widthForOneBox = widthFor11Boxes/11;\n                widthForLabel = widthForOneBox * chs.length + \"px\";\n              %>\n              <div class=\"ff-linear-scale-label-wrapper\">\n                <div class=\"ff-linear-scale-label-text\" style=\"text-align:<%-dir==='rtl'?'right':'left'%>; font-size:13px !important;width:<%-widthForLabel%>\">\n                  <%-item.scaleMin?item.scaleMin:''%>\n                </div>\n                <div class=\"ff-linear-scale-label-text\" style=\"text-align:<%-dir!=='rtl'?'right':'left'%>; font-size:13px !important;width:<%-widthForLabel%>\">\n                  <%-item.scaleMax?item.scaleMax:''%>\n                </div>\n              </div>\n            </div>\n          </div>\n          <% } else { %>\n          <!-- FOR DEFAULT -->\n          <div style=\"overflow-x: auto;\">\n            <table class=\"ff-widget-control <%-layout !== 'default' ? 'ff-linear-scale-hidden' : 'ff-linear-scale-default-container'%>\">\n              <col width=\"<%-Math.round(100/(chs.length+2))%>%\">\n              <% chs.forEach(function(ch){ %>\n                <col width=\"<%-Math.round(100/(chs.length+2))%>%\">\n              <% }) %>\n              <col width=\"*\">\n              <tr>\n                <td></td>\n                <% chs.forEach(function(ch, chi){ %>\n                  <td class=\"text-center\"><%-ch.value%><%-getScoreHint(chi)%></td>\n                <% }) %>\n                <td></td>\n              </tr>\n              <tr>\n                <td class=\"text-center ff-small-text\">\n                  <%-item.scaleMin?item.scaleMin:''%>\n                </td>\n                <% chs.forEach(function(ch){ %>\n                  <td class=\"text-center ff-pointer-cursor\" onclick=\"let input = this.querySelector('input');if(input) { input.click() }\" >\n                    <input onclick=\"var entr=<%-item.entry%>;if(formFacade.draft.entry[entr] == <%=ch.value%> && <%-item.required ? false : true%>){delete formFacade.draft.entry[entr]; document.getElementById('entry.<%-item.entry%>.<%=ch.value%>').checked = false;formFacade.saveDraft();}\" class=\"ff-scale ff-pointer-disable\" type=\"radio\" name=\"entry.<%-item.entry%>\" \n                      <%-item.required?'required':''%> <%-itmval==ch.value?'checked':''%> id=\"entry.<%-item.entry%>.<%=ch.value%>\" value=\"<%=ch.value%>\">\n                  </td>\n              <% }) %>\n              <td class=\"text-center ff-small-text\">\n                <%-item.scaleMax?item.scaleMax:''%>\n              </td>\n              </tr>\n            </table>\n          </div>\n          <% } %>\n          <% } else if(item.type=='GRID'){ %>\n            <% var chs = filter(item.choices) %>\n            <table class=\"ff-widget-control\">\n              <col width=\"*\">\n            <% chs.forEach(function(ch, chi){ %>\n              <col width=\"<%-Math.round(70/chs.length)%>%\">\n            <% }) %>\n            <tr>\n            <td></td>\n            <% chs.forEach(function(ch, chi){ %>\n              <td class=\"text-center\"><%-ch.value%><%-getScoreHint(chi)%></td>\n            <% }) %>\n            </tr>\n            <% item.rows.forEach(function(rw){ if(rw.multiple==1){ %>\n              <input type=\"hidden\" name=\"entry.<%-rw.entry%>_sentinel\"/>\n            <% } }) %>\n            <% \n              item.rows.forEach(function(rw, rwi){ \n                var rvals = draft.entry[rw.entry];\n                rvals = Array.isArray(rvals)?rvals:[rvals];\n            %>\n              <tr>\n              <td class=\"ff-grid-label\"><%-rw.value%></td>\n              <% chs.forEach(function(ch, chi){ %>\n              <td class=\"text-center ff-pointer-cursor\" onclick=\"let input = this.querySelector('input');if(input) { input.click() }\"><input class=\"ff-pointer-disable ff-grid-<%-rw.multiple==1?'checkbox':'radio'%> ff-grid-<%-item.entry%> ff-grid-<%-item.entry%>-row-<%-rwi%> ff-grid-<%-item.entry%>-col-<%-chi%> <%-item.onepercol?'ff-grid-onepercol':''%>\" type=\"<%-rw.multiple==1?'checkbox':'radio'%>\" name=\"entry.<%-rw.entry%>\" \n                    <%=rvals.indexOf(ch.value)>=0?'checked':''%> id=\"entry.<%-rw.entry%>.<%=ch.value%>\" value=\"<%=ch.value%>\" \n                    <% if(!rw.multiple){ %>\n                    onclick=\"entr=<%-rw.entry%>; if(formFacade.draft.entry[entr]==this.value){ delete formFacade.draft.entry[entr]; this.checked=false; formFacade.saveDraft(); }\"\n                    <% } %>\n                    <%-rw.multiple==0&&item.required?'required':''%>\n                    ></td>\n              <% }) %>\n              </tr>\n            <% }) %>\n            </table>\n          <% } else if(item.type=='IMAGE'){ %>\n            <% \n              var imageSrc;\n              if(!item.blob && item.image)\n                imageSrc = `https://formfacade.com/itemload/item/${encode(item.image)}`;\n              else\n                imageSrc =  `https://formfacade.com/itemembed/${params.publishId}/item/${item.id}/image/${item.blob}`;\n            %>\n            <img\n            <% if(data.util && data.util.getPlaceholder){ %>\n              src=\"<%-data.util.getPlaceholder(item.size)%>\" data-src=\"<%-imageSrc%>\"\n            <% } else{ %>\n              src=\"<%-imageSrc%>\" \n            <% } %>\n            alt=\"<%=itm.title?itm.title:(header.title?header.title:sec.title)%>\"\n            <% if(item.size){ %>\n              style=\"width:<%-item.size.width%>px;\n              <% if(item.size.align){ %> \n                margin-left:<%-item.size.align==0?'0px':'auto'%>; margin-right:<%-item.size.align==2?'0px':'auto'%>;\n              <% } %>\n              max-width:100%; height:auto;\"\n            <% } %>\n            class=\"lazyload ff-widget-control ff-full-image ff-image\" id=\"Widget<%-item.id%>\"/>\n          <% } else if(item.type=='VIDEO'){ %>\n            <div class=\"ff-widget-control ff-embed-responsive\">\n              <iframe class=\"ff-embed-responsive-item ff-video\" allowfullscreen\n                src=\"https://formfacade.com/itemembed/<%-params.publishId%>/item/<%-item.id%>/video/<%-item.blob?item.blob:'unknown'%>\"></iframe>\n            </div>\n          <% } else if(item.type=='DATE'){ %>\n            <% \n              var dates = getMinMaxDate(item, oitem);\n              var minDate = dates[0];\n              var maxDate = dates[1];\n            %>\n            <% if(item.time==1){ %>\n              <input type=\"datetime-local\" class=\"ff-widget-control <%-prepend('form-control')%>\" id=\"Widget<%-item.id%>\" \n                name=\"entry.<%-item.entry%>\" <%-item.required?'required':''%> value=\"<%=itmval%>\"\n                onBlur=\"if(event.target.value) event.target.value=event.target.value.substr(0, 16)\"\n                placeholder=\"yyyy-mm-ddTHH:mm\" pattern=\"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}\" min=\"<%-minDate%>\" max=\"<%-maxDate%>\">\n            <% } else{ %>\n              <% \n                var onchange = null;\n                if(oitem && oitem.validation && oitem.validation.validOperator === 'Weekly')\n                {\n                  onchange = `onchange=\"formFacade.weeklyDateValidation(${item.id}, this)\"`;\n                  if(oitem.validation.validFuture)\n                  {\n                    minDate = formatDate(new Date(), 'yyyy-MM-dd')\n                  }\n                } else if (oitem && oitem.validation && oitem.validation.validOperator === 'Exclude')\n                {\n                  if(oitem.validation.validFuture)\n                  {\n                    minDate = formatDate(new Date(), 'yyyy-MM-dd')\n                  }\n                  onchange = `onchange=\"formFacade.excludeDateValidation(${item.id}, this)\"`;\n                }\n                if(oitem.validation && oitem.validation.validOperator === 'Future')\n                {\n                  onchange = `onchange=\"formFacade.futureDateValidation(${item.id}, this)\"`;\n                }\n              %>\n              <input type=\"date\" class=\"ff-widget-control <%-prepend('form-control')%>\" id=\"Widget<%-item.id%>\" \n                name=\"entry.<%-item.entry%>\" <%-item.required?'required':''%> value=\"<%=itmval%>\"\n                placeholder=\"yyyy-mm-dd\" <%-onchange ? onchange : ''%> pattern=\"[0-9]{4}-[0-9]{2}-[0-9]{2}\" min=\"<%-minDate%>\" max=\"<%-maxDate%>\">\n            <% }  %>\n          <% } else if(item.type=='TIME'){ %>\n            <input type=\"time\" class=\"ff-widget-control <%-prepend('form-control')%>\" id=\"Widget<%-item.id%>\" \n              name=\"entry.<%-item.entry%>\" <%-item.required?'required':''%> value=\"<%=itmval%>\">\n          <% } else if(item.type=='SECTION_HEADER'){ %>\n          <% } else { %>\n            <input type=\"text\" class=\"ff-widget-control <%-prepend('form-control')%>\" id=\"Widget<%-item.id%>\" \n              name=\"entry.<%-item.entry%>\" <%-item.required?'required':''%> value=\"<%=itmval%>\">\n          <% } %>\n          <% \n            if(item.product){ \n              var cartsm;\n              if(item.type=='GRID')\n              {\n                var rws = item.rows?item.rows:[];\n                var rwvals = rws.map(function(rw, rwi){\n                    var rwval = draft.entry[rw.entry];\n                    if(!rwval) return;\n                    if(Array.isArray(rwval))\n                      return rw.value+' : '+rwval.join(', ');\n                    else if(isNaN(rwval))\n                      return rw.value+' : '+rwval;\n                    else\n                      return rw.value+' x <b>'+rwval+'</b>';\n                }).filter(rwval=>rwval);\n                if(rwvals.length>0)\n                  cartsm = rwvals.map(ln=>'<div class=\"ff-sm-line\">'+ln+'</div>').join('\\n');\n              }\n              else if(itmval && item.type=='PARAGRAPH_TEXT')\n              {\n                var rws = [];\n                var cfg = toConfigurable(itmval);\n                cfg.configItem.forEach(ci=>{\n                  for(var vid in ci.lineItem)\n                  {\n                    var qty = ci.lineItem[vid]||1;\n                    var vrns = oitem.variants||{};\n                    if(vid.includes(\"-\"))\n                    {\n                      var [v, v1] = vid.split('-');\n                      var vrns1 = oitem.variants1 || {};\n                      if(vrns && vrns1 && vrns[v] && vrns1[v1])\n                      {\n                        var vrnm = vrns[v].name + ' | ' + vrns1[v1].name;\n                        rws.push(vrnm+' x <b>'+qty+'</b>');\n                        continue;\n                      }\n                    }\n                    if(oitem.measure !== 'Nested') {\n                      var vrn = vrns[vid]||{};\n                      var vrnm = vrn.name||vid;\n                      rws.push(vrnm+' x <b>'+qty+'</b>');\n                    }\n                  }\n                });\n                if(rws.length>0)\n                  cartsm = rws.map(ln=>'<div class=\"ff-sm-line\">'+ln+'</div>').join('\\n');\n              }\n              else if(itmval)\n              {\n                if(Array.isArray(itmval))\n                {\n                  cartsm = itmval.filter(vl=>vl).join(' | ');\n                }\n                else if(isNaN(itmval))\n                {\n                  cartsm = itmval;\n                }\n                else\n                {\n                  var frmtval = oitem&&oitem.measure=='Weight'?formatWeight(itmval):itmval;\n                  cartsm = 'x <b>'+frmtval+'</b>';\n                }\n              }\n          %>\n            <% if(cartsm){ %>\n              <div <%-oncartClick%> class=\"ff-add-cart\">\n                <!-- Shopping cart SVG -->\n                <svg xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 -960 960 960\" width=\"24\"><path d=\"M440-600v-120H320v-80h120v-120h80v120h120v80H520v120h-80ZM280-80q-33 0-56.5-23.5T200-160q0-33 23.5-56.5T280-240q33 0 56.5 23.5T360-160q0 33-23.5 56.5T280-80Zm400 0q-33 0-56.5-23.5T600-160q0-33 23.5-56.5T680-240q33 0 56.5 23.5T760-160q0 33-23.5 56.5T680-80ZM280-280q-45 0-69-39.5t-1-78.5l54-98-144-304H40v-80h131l170 360h281l155-280 70 38-155 280q-11 20-29 31t-41 11H324l-44 80h480v80H280Z\"/></svg>\n              </div>\n              <div <%-oncartClick%>  class=\"ff-sel-cart\">\n                <div class=\"ff-sel-cart-sm\"><%-cartsm%></div>\n              </div>\n            <% } else if(oitem.inventory=='yes' && oitem.remain<=0){ %>\n              <div <%-oncartClick%> class=\"ff-add-cart\">\n                <!-- Shopping cart SVG -->\n                <svg xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 -960 960 960\" width=\"24\"><path d=\"M440-600v-120H320v-80h120v-120h80v120h120v80H520v120h-80ZM280-80q-33 0-56.5-23.5T200-160q0-33 23.5-56.5T280-240q33 0 56.5 23.5T360-160q0 33-23.5 56.5T280-80Zm400 0q-33 0-56.5-23.5T600-160q0-33 23.5-56.5T680-240q33 0 56.5 23.5T760-160q0 33-23.5 56.5T680-80ZM280-280q-45 0-69-39.5t-1-78.5l54-98-144-304H40v-80h131l170 360h281l155-280 70 38-155 280q-11 20-29 31t-41 11H324l-44 80h480v80H280Z\"/></svg>\n              </div>\n              <div <%-oncartClick%> class=\"ff-sel-cart\">\n                <div class=\"ff-sel-cart-sm\"><%-lang('Out of stock')%></div>\n              </div>\n            <% } else{ %>\n              <div <%-oncartClick%> class=\"ff-add-cart\">\n                <!-- Shopping cart SVG -->\n                <svg xmlns=\"http://www.w3.org/2000/svg\" height=\"24\" viewBox=\"0 -960 960 960\" width=\"24\"><path d=\"M440-600v-120H320v-80h120v-120h80v120h120v80H520v120h-80ZM280-80q-33 0-56.5-23.5T200-160q0-33 23.5-56.5T280-240q33 0 56.5 23.5T360-160q0 33-23.5 56.5T280-80Zm400 0q-33 0-56.5-23.5T600-160q0-33 23.5-56.5T680-240q33 0 56.5 23.5T760-160q0 33-23.5 56.5T680-80ZM280-280q-45 0-69-39.5t-1-78.5l54-98-144-304H40v-80h131l170 360h281l155-280 70 38-155 280q-11 20-29 31t-41 11H324l-44 80h480v80H280Z\"/></svg>\n              </div>\n            <% } %>\n            <%\n              if(layout === 'default' && oitem.measure === 'Nested') {\n                var vrns = Object.keys(oitem.variants || {});\n                var vrns1 = Object.keys(oitem.variants1 || {});\n                var max = 8;\n                var multisel = oitem.multiselect === 'yes';\n                if(oitem && oitem.choices && oitem.choices.length > 0) {\n                  var lastChoice = oitem.choices[oitem.choices.length - 1];\n                  if(typeof lastChoice === 'string') lastChoice= Number(lastChoice);\n                  max = Math.min(max, lastChoice);\n                }\n            %>\n              <% if(max > 1) { %>\n              <table class=\"ff-widget-control\">\n                <col width=\"*\">\n                <% for(var i = 0; i < max; i++) { %>\n                <col width=\"<%-Math.round(70/max)%>%\">\n                <% } %>\n                <tr>\n                  <td></td>\n                  <% for(var i = 0; i < max; i++) { %>\n                  <td class=\"text-center\"><%-i + 1%></td>\n                  <% } %>\n                </tr>\n                <%\n                var variantConfig = oitem.variantConfig || {};\n                var inventory = variantConfig.inventory || {};\n                for (var i = 0; i < vrns.length; i++) {\n                  for (var j = 0; j < vrns1.length; j++) {\n                    var v = vrns[i];\n                    var v1 = vrns1[j];\n                    var vname = oitem.variants[v].name;\n                    var v1name = oitem.variants1[v1].name;\n                    var vrnm = vname + ' + ' + v1name;\n                    var vid = v + '-' + v1;\n                    var inv = inventory[v + \"-\" + v1] || {};\n                    var remain = inv.remain;\n                    if(remain === undefined) {\n                      remain = \"\";\n                    }\n                %>\n                <tr>\n                  <td class=\"ff-grid-label\"><%-vrnm%></td>\n                  <% \n                  for(var k = 0; k < max; k++) { \n                    var chi = k, ch = chi + 1;\n                    var isOutOfStock = true;\n                    if(remain === \"\" || remain >= ch) isOutOfStock = false;\n                    var vsel = false;\n                    if(cfg && cfg.configItem && cfg.configItem[0].lineItem && cfg.configItem[0].lineItem[vid] == ch) {\n                      vsel = true;\n                    }\n                  %>\n                    <td class=\"text-center <%=isOutOfStock?'ff-pointer-not-allowed': 'ff-pointer-cursor'%>\" onclick=\"let input = this.querySelector('input');if(input) { input.click() }\"><input class=\"ff-pointer-disable ff-grid-<%-multisel?'checkbox':'radio'%> ff-grid-<%-item.entry%> ff-grid-<%-item.entry%>-row-<%-vid%> ff-grid-<%-item.entry%>-col-<%-chi%>\" <%-isOutOfStock ? 'disabled' : ''%> type=\"radio\" name=\"variant.<%-item.entry%><%-multisel?('.'+vid):''%>\" onclick=\"var cfg = formFacade.toConfigurable(document.getElementById('Widget<%-item.id%>').value);if(cfg.configItem[0].lineItem['<%=vid%>']=='<%-ch%>'){ this.checked=false; formFacade.saveDraft(event); }\" <%=vsel?'checked':''%> id=\"entry.<%-vid%>.<%=ch%>\" value=\"<%=vname%> | <%=v%> || <%-v1name%> | <%-v1%> * <%=ch%>\"\n                    >\n                    </td>\n                  <% } %>\n                </tr>\n              <% } } %>\n              </table>\n              <% } else if (max === 1) { %>\n                <table class=\"ff-widget-control\">\n                  <col width=\"*\">\n                  <% for (var i = 0; i < vrns1.length; i++) { %>\n                  <col width=\"<%-Math.round(70/vrns1.length)%>%\">\n                  <% } %>\n                  <tr>\n                    <td></td>\n                    <% for(var i = 0; i < vrns1.length; i++) { %>\n                    <td class=\"text-center\"><%-oitem.variants1[vrns1[i]].name%></td>\n                    <% } %>\n                  </tr>\n                  <%\n                  var variantConfig = oitem.variantConfig || {};\n                  var inventory = variantConfig.inventory || {};\n                  for (var i = 0; i < vrns.length; i++) {\n                    var v = vrns[i];\n                    var vrnm = oitem.variants[v].name;\n                  %>\n                  <tr>\n                    <td class=\"ff-grid-label\"><%-vrnm%></td>\n                    <% \n                    for(var j = 0; j < vrns1.length; j++) { \n                      var chi = j, ch = 1;\n                      var v1 = vrns1[j];\n                      var isOutOfStock = true;\n                      var vid = v + '-' + v1;\n                      var inv = inventory[vid] || {};\n                      var remain = inv.remain;\n                      var vrnm1 = oitem.variants1[v1].name;\n                      if(remain === undefined) {\n                        remain = \"\";\n                      }\n                      if(remain === \"\" || remain > 0) isOutOfStock = false;\n                      var vsel = false;\n                      if(cfg && cfg.configItem && cfg.configItem[0].lineItem && cfg.configItem[0].lineItem[vid] == ch) {\n                        vsel = true;\n                      }\n                    %>\n                      <td class=\"text-center <%=isOutOfStock?'ff-pointer-not-allowed': 'ff-pointer-cursor'%>\" onclick=\"let input = this.querySelector('input');if(input) { input.click() }\"><input class=\"ff-pointer-disable ff-grid-<%-multisel?'checkbox':'radio'%> ff-grid-<%-item.entry%> ff-grid-<%-item.entry%>-row-<%-vid%> ff-grid-<%-item.entry%>-col-<%-chi%>\" <%-isOutOfStock ? 'disabled' : ''%> type=\"radio\" name=\"variant.<%-item.entry%><%-multisel?('.'+vid):''%>\" onclick=\"var cfg = formFacade.toConfigurable(document.getElementById('Widget<%-item.id%>').value); if(cfg.configItem[0].lineItem['<%=vid%>']=='<%-ch%>'){ this.checked=false; formFacade.saveDraft(event); }\" <%=vsel?'checked':''%> id=\"entry.<%-vid%>.<%=ch%>\" value=\"<%=vrnm%> | <%=v%> || <%-vrnm1%> | <%-v1%> * <%=ch%>\"\n                      >\n                      </td>\n                    <% } %>\n                  </tr>\n                <% } %>\n                </table>\n              <% } %>\n            <% } else if(layout=='default' && item.type=='PARAGRAPH_TEXT'){\n                var cfg = toConfigurable(itmval).configItem[0];\n                var multisel = oitem.multiselect=='yes';\n                var vrns = oitem.variants||{};\n                var ochs = oitem.choices||[];\n                if(ochs.length==1){\n                  var isradio = !multisel&&Object.keys(vrns).length>1;\n            %>\n                <div class=\"ff-widget-control ff-check-table\">\n                <%\n                  var och = ochs[0];\n                  for(var vid in vrns){ \n                    var ovrn = vrns[vid]||{};\n                    var vsel = cfg.lineItem[vid];\n                %>\n                  <div class=\"ff-form-check\">\n                    <input class=\"ff-form-check-input\" type=\"<%-isradio?'radio':'checkbox'%>\" name=\"variant.<%-item.entry%>\" id=\"variant.<%-item.entry%>.<%-vid%>\" \n                      <% if(isradio){ %>\n                      onclick=\"var cfg = formFacade.toConfigurable(document.getElementById('Widget<%-item.id%>').value); if(cfg.configItem[0].lineItem[<%-vid%>]){ this.checked=false; formFacade.saveDraft(event); }\"\n                      <% }%>\n                      <%-vsel?'checked':''%> value=\"<%=ovrn.name%> | <%-vid%> * <%-och%>\">\n                    <label class=\"ff-form-check-label\" for=\"variant.<%-item.entry%>.<%-vid%>\">\n                      <%-ovrn.display||ovrn.name%>\n                    </label>\n                  </div>\n                <% } %>\n                </div>\n            <% \n                }\n                else\n                {\n            %>\n                <table class=\"ff-widget-control\">\n                  <col width=\"*\">\n                <% ochs.forEach(function(ch){ %>\n                  <col width=\"<%-Math.round(70/ochs.length)%>%\">\n                <% }) %>\n                <tr>\n                <td></td>\n                <% ochs.forEach(function(ch){ %>\n                  <td class=\"text-center\"><%-ch%></td>\n                <% }) %>\n                </tr>\n                <% \n                  for(var vid in vrns){ \n                    var ovrn = vrns[vid]||{};\n                    var vsel = cfg.lineItem[vid];\n                %>\n                  <tr>\n                  <td class=\"ff-grid-label\"><%-ovrn.display||ovrn.name%></td>\n                  <% ochs.forEach(function(ch, chi){ %>\n                  <% \n                    var isOutOfStock = false;\n                    if(oitem.inventory=='yes' && ovrn.remain) \n                    {\n                      if(Number(ovrn.remain) < ch)\n                      {\n                        isOutOfStock = true;\n                      }\n                    }\n                  %>\n                  <td class=\"text-center <%=isOutOfStock?'ff-pointer-not-allowed': 'ff-pointer-cursor'%>\" onclick=\"let input = this.querySelector('input');if(input) { input.click() }\"><input class=\"ff-pointer-disable ff-grid-<%-multisel?'checkbox':'radio'%> ff-grid-<%-item.entry%> ff-grid-<%-item.entry%>-row-<%-vid%> ff-grid-<%-item.entry%>-col-<%-chi%>\" <%= isOutOfStock ? 'disabled' : ''%> type=\"radio\" name=\"variant.<%-item.entry%><%-multisel?('.'+vid):''%>\"\n                      onclick=\"var cfg = formFacade.toConfigurable(document.getElementById('Widget<%-item.id%>').value); if(cfg.configItem[0].lineItem[<%-vid%>]=='<%-ch%>'){ this.checked=false; formFacade.saveDraft(event); }\" \n                        <%=vsel==ch?'checked':''%> id=\"entry.<%-vid%>.<%=ch%>\" value=\"<%=ovrn.name%> | <%=vid%> * <%=ch%>\" \n                        ></td>\n                  <% }) %>\n                  </tr>\n                <% } %>\n                </table>\n            <% \n                }\n              } \n            %>\n          <% } %>\n          <div id=\"Error<%-item.id%>\" class=\"ff-widget-error\"></div>\n        </div>\n      <% }) %>\n      <% \n        if(isEditMode() && hasCreatorOrEditor() && !header.outstock){ \n          var aftr;\n          if(header.items.length==0)\n            aftr = header.head?header.head.id:sec.id;\n          else\n            aftr = item?item.id:'root';\n          var hdprds = header.items.filter(itm=>itm.product);\n          var isprd = hdprds.length>=(header.items.length-hdprds.length);\n          var productNoun = fac.product ? fac.product.type : 'product';\n          if(hdprds.length==0 && (s==0||s+1==sections.length)) isprd = false;\n          var hdamt = header.items.filter(itm=>itm.id==(fac.mapping||{}).amount);\n      %>\n          <div class=\"ff-create\" id=\"ff-append-<%-aftr%>\">\n            <div class=\"btn-group\">\n              <% if(isprd && hasCreator()) { %>\n                <button type=\"button\" class=\"btn ff-next\" onclick=\"editFacade.afterLoad('createProduct', {insertAfter:'<%-aftr%>', type:'product'})\">Add product</button>\n              <% } else if(hdamt.length>0 && hasCreator()) { %>\n                <button type=\"button\" class=\"btn ff-next\" onclick=\"editFacade.afterLoad('createPrice', null, {insertAfter:'<%-aftr%>', type:'service'})\">Add price field</button>\n              <% } else { %>\n                <button type=\"button\" class=\"btn ff-next\" onclick=\"editFacade.afterLoad('create', null, {insertAfter:'<%-aftr%>', type:'field'})\">Add field</button>\n              <% } %>\n              <button type=\"button\" class=\"btn ff-next dropdown-toggle dropdown-toggle-split\" data-toggle=\"dropdown\" aria-haspopup=\"true\" aria-expanded=\"false\">\n                <span class=\"sr-only\">Toggle Dropdown</span>\n              </button>\n              <div class=\"dropdown-menu\">\n                <% if(hasCreator()){ %>\n                  <a class=\"dropdown-item\" href=\"javascript:void(0)\" onclick=\"editFacade.afterLoad('createProduct', {insertAfter:'<%-aftr%>', type:'product'})\">Add product</a>\n                  <a class=\"dropdown-item\" href=\"javascript:void(0)\" onclick=\"editFacade.afterLoad('createPrice', null, {insertAfter:'<%-aftr%>', type:'service'})\">Add price field</a>\n                <% } %>\n                <a class=\"dropdown-item\" href=\"javascript:void(0)\" onclick=\"editFacade.afterLoad('create', null, {insertAfter:'<%-aftr%>', type:'field'})\">Add field</a>\n                <a class=\"dropdown-item\" href=\"javascript:void(0)\" onclick=\"editFacade.afterLoad('confirmCreate', null, {insertAfter:'<%-aftr%>', type:'field', widget:'SectionHeaderItem'})\">Add header</a>\n              </div>\n            </div>\n          </div>\n      <% } %>\n    <% }) %>\n    </div>\n\n    <div class=\"ff-button-bar\">\n    <% if(s>=1){ %>\n      <button type=\"button\" class=\"<%-backcss%> ff-back\" id=\"ff-back-<%-sec.id%>\"\n        onclick=\"formFacade.gotoSection(this.form, '<%-sec.id%>', 'back')\">\n        <!-- arrow back SVG -->\n        <% if(dir === 'ltr') { %>\n        <span class=\"material-icons\">arrow_back</span>\n        <% } else { %>\n        <span class=\"material-icons\">arrow_forward</span>\n        <% } %>\n        <span><%-lang('Back')%></span>\n      </button>\n    <% } %>\n    <% \n      if(frm.errorMessage){\n      } else if(s+1==sections.length || sec.next==-3){ \n        data.ending = sec.id;\n        var waphone = getPhone(sec.id);\n        var itmsubmit = fac.submit?fac.submit[sec.id]:null;\n        var onclick = 'formFacade.submitWithInventory(this.form, \\''+sec.id+'\\')';\n        if(isEditMode() && s>=1)\n          onclick = 'formFacade.launchPreview()';\n        else if(itmsubmit && itmsubmit.payConfig=='peergateway' && !draft.responseId && !draft.submitSeq)\n          onclick = 'formFacade.showPaymentWithCheckInventory(this.form, \\''+sec.id+'\\')';\n        else if(itmsubmit && itmsubmit.amountFrom && !draft.responseId && !draft.submitSeq)\n          onclick = 'formFacade.showPayment(this.form, \\''+sec.id+'\\')';\n      %>\n        <button type=\"button\" class=\"<%-submitcss%> ff-submit\" id=\"ff-submit-<%-sec.id%>\" onclick=\"<%-onclick%>\">\n          <% var rtlsubmitStyle = \"\"; %>\n          <% if(dir === 'rtl') { %>\n          <% \n            rtlsubmitStyle = \"-webkit-transform: scaleX(-1);transform: scaleX(-1);\";\n          %>\n          <% } %>\n          <% if(waphone){ %>\n            <img alt=\"Submit\" src=\"https://formfacade.com/img/wa.svg\" class=\"ff-submit-icon\"/>\n          <% } else { %>\n            <img alt=\"Submit\" style=\"<%=rtlsubmitStyle%>\" src=\"https://formfacade.com/img/send.svg\" class=\"ff-submit-icon\"/>\n          <% } %>\n          <span>\n            <%-itmsubmit&&itmsubmit.displayName?itmsubmit.displayName:lang(waphone?'Send message':'Submit')%>\n          </span>\n        </button>\n      <% if(isEditMode()){ %>\n        <i class=\"ff-customize material-icons\" onclick=\"editFacade.afterLoad('confirmSubmit', '<%-sec.id%>')\">settings</i>\n      <% } %>\n    <% } else { %>\n        <button type=\"button\" class=\"<%-submitcss%> ff-next\" id=\"ff-next-<%-sec.id%>\"\n          onclick=\"formFacade.gotoSection(this.form, '<%-sec.id%>', '<%-sec.next%>')\">\n          <% \n            var nxtsec = sec.next==-2?sections[s+1]:null;\n            if(nxtsec && nxtsec.id==fac.setting.loginpage){ \n          %>\n            <span class=\"material-icons\">lock</span>\n            <span><%-lang('Next')%></span>\n          <% } else{ %>\n            <span><%-lang('Next')%></span>\n            <!-- arrow forward SVG -->\n            <% if(dir === 'ltr') { %>\n              <span class=\"material-icons\">arrow_forward</span>\n            <% } else { %>\n              <span class=\"material-icons\">arrow_back</span>\n            <% } %>\n          <% } %>\n        </button>\n        <% if(isEditMode() && hasCreatorOrEditor()){ %>\n          <i class=\"ff-customize material-icons\" onclick=\"editFacade.afterLoad('showNext', '<%-sec.id%>')\">settings</i>\n        <% } %>\n    <% } %>\n\n    <% \n      var inlinecss = {display:'block', position:'relative', opacity:1, visibility:'visible',\n        'font-size':'1em', 'font-weight':600, 'line-height':'1.25', 'letter-spacing':'0.25px', 'z-index': 9999, padding: '1.125em 12px'};\n      var inlinestyle = Object.keys(inlinecss).map(function(ky){ return ky+':'+inlinecss[ky]+' !important'; }).join('; ');\n    %>\n    <% if(!params.userId){ %>\n      <a href=\"https://formfacade.com/verify-google-forms-ownership.html\" target=\"_blank\"\n      class=\"ff-powered\" style=\"color:#0074D9 !important; <%-inlinestyle%>\">\n        Ownership not verified\n      </a>\n    <% } else if(isEditMode()){ %>\n      <% if(hasCreator() && (fac.neartail || fac.whatsapp)){ %>\n          <span class=\"material-icons ff-jump-nav\" onclick=\"formFacade.showNavigation()\">apps</span>\n      <% } %>\n    <% } else{ %>\n      <% \n        if(!config.plan || config.branded){ \n          var prd = (fac.formesign||fac.hipaache)?'Formesign':(fac.neartail||fac.whatsapp)?'Neartail':'Formfacade';\n      %>\n        <% if(!config.plan && prd=='Formfacade' && config.installAt<1702767600000 && config.usage>=100 && config.trial>7){ %>\n          <a href=\"<%-reurl%>&utm_content=oldlimit\" \n            <% if(config.usage>=120){ %>\n              class=\"ff-blocked\" style=\"color:#fff !important; <%-inlinestyle%>\"\n            <% } else{ %>\n              class=\"ff-warned\" style=\"color:#000 !important; border:1px solid #f5c6cb !important; <%-inlinestyle%>\"\n            <% } %>\n            target=\"_blank\">\n            <b style=\"margin-right: 5px\">âš </b> Form responses limit reached. Upgrade now.\n          </a>\n        <% } else if(!config.plan && config.usage>=20 && config.trial>7){ %>\n          <a href=\"<%-reurl%>&utm_content=limit\"\n            <% if(config.usage>=30){ %>\n              class=\"ff-blocked\" style=\"color:#fff !important; <%-inlinestyle%>\"\n            <% } else{ %>\n              class=\"ff-warned\" style=\"color:#000 !important; border:1px solid #f5c6cb !important; <%-inlinestyle%>\"\n            <% } %>\n            target=\"_blank\">\n            <b>âš </b> Form responses limit reached. Upgrade now.\n          </a>\n        <% } else{ %>\n          <a href=\"<%-reurl%>&utm_content=logo\" target=\"_blank\" \n            class=\"ff-powered-img\" style=\"display:inline-block !important; position: relative !important;height: auto; \"\n            title=\"<%-config.plan?('Powered by '+prd):'Try it for Free'%>\">\n            <% if(fac.hipaache || fac.formesign || fac.mailrecipe){ %>\n                          <% } %>\n          </a>\n        <% } %>\n      <% } else if(config.plan=='paid'){ %>\n        <%\n          var nxtsec;\n          var ctgs = sections.map(function(ctgsec,c){\n            if(ctgsec.id==sec.id) nxtsec = sections[c+1];\n            var itmnext = fac.next?fac.next[ctgsec.id]:null;\n            if(!itmnext) itmnext = fac.submit?fac.submit[ctgsec.id]:null;\n            if(itmnext && itmnext.navigation=='added')\n              return ctgsec;\n          }).filter(ctgsec=>ctgsec);\n        %>\n        <% if(hasCreator() && ctgs.length>1 && itmnxt && itmnxt.navigation=='added'){ %>\n          <span class=\"material-icons ff-jump-nav\" onclick=\"formFacade.showNavigation()\">apps</span>\n        <% } %>\n      <% } else if(config.plan=='warned'){ %>\n        <a href=\"<%-reurl%>&utm_content=warned\" target=\"_blank\" \n          class=\"ff-warned\" style=\"color:#000 !important; border:1px solid #f5c6cb !important; <%-inlinestyle%>\">\n          <b>âš¡</b> Form responses limit reaching soon\n        </a>\n      <% } else if(config.plan=='blocked'){ %>\n        <a href=\"<%-reurl%>&utm_content=blocked\" target=\"_blank\" \n          class=\"ff-blocked\" style=\"color:#fff !important; <%-inlinestyle%>\">\n          <b>âš </b> Form responses limit reached. Upgrade now.\n        </a>\n      <% } %>\n    <% } %>\n    </div>\n    </div>\n\n    <% if(isEditMode() && hasCreatorOrEditor()){ %>\n      <div class=\"ff-add-page\">\n        <button class=\"btn btn-lg ff-back\" onclick=\"editFacade.afterLoad('confirmCreate', null, {insertAfter:'<%-item?item.id:'root'%>', type:'field', widget:'PageBreakItem'})\" title=\"Insert new page\">\n          <span class=\"material-icons\">\n            add_circle_outline\n          </span>\n          Add page\n        </button>\n      </div>\n    <% } %>\n\n    <% }) %>\n\n    <% \n      var waphone = getPhone();\n      var firstsec = sections[0]||{};\n    %>\n    <div class=\"ff-section\" id=\"ff-sec-ending\" style=\"<%-draft.activePage=='ending'?'display:block':'display:none'%>\">\n    <div class=\"ff-secfields\">\n      <h3 class=\"ff-title\"><%-html(firstsec.title||frm.title)%></h3>\n      <p style=\"padding-bottom:80px;\">Click <%-lang(waphone?'Send message':'Submit')%> to finish.</p>\n    </div>\n    <div class=\"ff-button-bar\">\n      <button type=\"button\" class=\"<%-backcss%> ff-back\" \n        onclick=\"formFacade.gotoSection(this.form, '<%-data.ending%>', 'back')\">\n        <!-- arrow back SVG -->\n        <% if(dir === 'ltr') { %>\n          <span class=\"material-icons\">arrow_back</span>\n        <% } else { %>\n          <span class=\"material-icons\">arrow_forward</span>\n        <% } %>\n        <span><%-lang('Back')%></span>\n      </button>\n      <button type=\"button\" class=\"<%-submitcss%> ff-submit\"\n        onclick=\"formFacade.submitWithInventory(this.form, '-3')\">\n        <!-- send SVG -->\n        <svg xmlns=\"http://www.w3.org/2000/svg\" height=\"19\" viewBox=\"0 -960 960 960\" width=\"19\" role=\"img\" aria-label=\"Submit\"><path d=\"M120-160v-640l760 320-760 320Zm80-120 474-200-474-200v140l240 60-240 60v140Zm0 0v-400 400Z\"/></svg>\n        <span><%-lang(waphone?'Send message':'Submit')%></span>\n      </button>\n    </div>\n    </div>\n    <% if (fac && fac.setting && fac.setting.progressBar === 'on') { %>\n      <div class=\"ff-progressbar\">\n        <div class=\"ff-page-progress-bar\">\n          <progress id=\"ff-page-progress\" value=\"1\" max=\"<%-sections.length%>\"></progress>\n        </div>\n        <div class=\"ff-page-progress-label\">\n          Page <span id=\"ff-page-progress-start-label\">1</span> of <%=sections.length%>\n        </div>\n      </div>\n    <% } %>\n  </form>\n<% } %>\n\n<%\n  var paybtns = getPaymentButtons();\n  paybtns.forEach(paybtn=>{\n    var itm = frmitms[paybtn.amountFrom]||{};\n    var amt = draft.entry[itm.entry]||0;\n    var txtamt = itm.format?itm.format(amt):amt;\n%>\n  <div id=\"ff-payment-list-<%-paybtn.id%>\" class=\"ff-payment-form ff-form ff-layout-<%-layout%>\"\n    style=\"<%-draft.activePage==(paybtn.id+'-paylist')?'display:block':'display:none'%>\">\n    <div class=\"ff-section\">\n      <div class=\"ff-title\">\n        <%-lang('Pay using')%>\n      </div>\n      <div><%-txtamt%></div>\n      <div class=\"ff-paylist\">\n      <% if(paymentIntent){ %>\n        <% paymentIntent.payment_method_types.forEach((typ,t)=>{ %>\n          <div class=\"ff-payoption\" onclick=\"formFacade.showPaymentMethod(<%-t%>, '<%-paybtn.id%>')\">\n            <div class=\"ff-paylogo\"><img src=\"/img/payment/<%-typ.logo?typ.logo:'money.png'%>\"/></div>\n            <div class=\"ff-payname\"><%-typ.name%></div>\n            <div class=\"material-icons\">delete</div>\n          </div>\n        <% }) %>\n      <% } %>\n      </div>\n    </div>\n  </div>\n  <form id=\"ff-payment-form-<%-paybtn.id%>\" class=\"ff-payment-form ff-form ff-layout-<%-layout%>\"\n    style=\"<%-draft.activePage==(paybtn.id+'-pay')?'display:block':'display:none'%>\">\n    <div class=\"ff-section\">\n      <div class=\"ff-title\">\n        <%-lang('Secure checkout')%>\n        <span style=\"float:right;\"><%-txtamt%></span>\n      </div>\n      <div id=\"ff-card-element-<%-paybtn.id%>\" class=\"<%-prepend('form-control')%>\" style=\"padding:12px; height:48px;\">\n        Loading...\n      </div>\n      <label for=\"ff-card-element-<%-paybtn.id%>\" style=\"padding-top:12px; padding-bottom:4px;\">\n        All transactions are safe and secure. \n        Credit card details are not stored.\n      </label>\n      <div id=\"ff-card-errors-<%-paybtn.id%>\" role=\"alert\" style=\"color:red; padding-bottom:4px;\"></div>\n      <button type=\"submit\" class=\"<%-submitcss%> ff-submit\" id=\"ff-pay-<%-paybtn.id%>\" onclick=\"\">\n        <!-- send SVG -->\n        <svg xmlns=\"http://www.w3.org/2000/svg\" height=\"19\" viewBox=\"0 -960 960 960\" width=\"19\" role=\"img\" aria-label=\"Paynow\"><path d=\"M120-160v-640l760 320-760 320Zm80-120 474-200-474-200v140l240 60-240 60v140Zm0 0v-400 400Z\"/></svg>\n        <span><%-lang('Pay Now')%></span>\n      </button>\n    </div>\n  </form>\n  <div id=\"ff-payment-confirm-<%-paybtn.id%>\" class=\"ff-payment-form ff-form ff-layout-<%-layout%>\"\n    style=\"<%-draft.activePage==(paybtn.id+'-payconfirm')?'display:block':'display:none'%>\">\n    <div class=\"ff-section\">\n      <div class=\"ff-title\">\n        <%-lang('Payment successful')%>\n      </div>\n      <div><%-lang('Placing your order...')%></div>\n    </div>\n  </div>\n<% }) %>\n\n<div id=\"ff-addprd-overlay\" onclick=\"formFacade.closePopup()\"></div>\n<div id=\"ff-addprd-popup\" dir=\"<%=dir%>\"></div>\n","summary":"<%\n\tvar form = data.scraped;\n%>*<%-form.title?form.title:form.form%> #<%-formFacade.draft.submitSeq%>*\n<%\n\tvar vals = {};\n\tif(formFacade.draft && formFacade.draft.entry)\n\t\tvals = formFacade.draft.entry;\n\tvar items = Object.values(form.items).sort((a,b)=>a.index-b.index);\n    var oitems = data.facade&&data.facade.items?data.facade.items:{};\n\titems.forEach(item=>{\n\t\tvar oitem = oitems[item.id];\n\t\tif(!oitem) oitem = {};\n\t\tvar val = vals[item.entry];\n        if(val && val.length==0) val = null;\n        if(item.type=='GRID')\n        {\n                item.rows.forEach(function(rw, r){\n                \tvar rvals = vals[rw.entry];\n                    if(rvals)\n                    {\n\t\t\t\t\t\trvals = Array.isArray(rvals)?rvals:[rvals];\n%>\n*<%-rw.value%> :* <%-rvals.join(', ')%><%\n                    }\n                });\n        }\n\t\telse if(item.title && val)\n\t\t{\n\t\t\tif(oitem.mode=='hide'){}\n\t\t\telse if(oitem.calculated && oitem.calculated.indexOf('${textsummary')==0){}\n\t\t\telse{\n\t\t\t\tif(Array.isArray(val) || val=='__other_option__')\n\t\t\t\t{\n\t\t\t\t\tval = Array.isArray(val)?val:[val];\n\t\t\t\t\tval = val.map(function(vl){\n\t\t\t\t\t\tif(vl=='__other_option__')\n\t\t\t\t\t\t\tvl = vals[item.entry+'-other_option_response'];\n\t\t\t\t\t\treturn vl;\n\t\t\t\t\t});\n\t\t\t\t}\n\t\t\t\telse if(item.format)\n\t\t\t\t{\n\t\t\t\t\tval = item.format(val);\n\t\t\t\t}\n%>\n*<%-item.title%> :* <%-Array.isArray(val)?val.join(', '):val%><% \n\t\t\t}\n\t\t}\n\t})\n%>\n<% if(!config.plan){ %>-\nMade with WhatsTarget.com\n-<% } %>\n(Press send to confirm)","preview":"\n<style>\n\t:root{\n\t\t--ff-bgcolor: #fff;\n\t\t--ff-font-color: #202124;\n\t    --ff-font-size: 14px;\n\t    --ff-head-size: 31.5px;\n\t    --ff-font-small: 12px;\n\t    --ff-field-bgcolor: #fff;\n\t\t--ff-gray-900: #202124;\n\t}\n    .ff-preview .ff-blur{ filter:blur(3px); -webkit-filter:blur(3px); }\n</style>\n\n<form id=\"Publish_preview\" class=\"ff-form ff-layout-default ff-public-mode ff-preview\">\n\n    <div class=\"ff-section\" id=\"ff-sec-root\">\n\n\t\t\n\n\t\t<h3 class=\"h3 ff-title \" id=\"ff-title-root\">\n\t\t\tContact Us\n\t\t</h3>\n      \n        <div class=\"ff-description ff-blur\" id=\"ff-desc-root\">\n        \t<p>Loading your form content...<p>\n        </div>\n      \n    \t<div class=\"ff-secfields\">\n    \t\t<img src=\"https://formfacade.com/banner/loadingform.gif\" loading=\"lazy\" class=\"ff-title-image ff-image\">\n        </div>\n\n    </div>\n\n</form>\n"}

formFacade.config = {"shortId":"gJUrHA_TU","usage":4,"trial":1157,"setting":{},"installAt":1624890930462,"themecolor":"colorful-5d33fb","themecss":"font=%22Roboto%22%2C%20sans-serif&heading=%22Poppins%22%2C%20sans-serif&primary=%235d33fb&primaryActive=%23492bbb&secondary=%23b161fc","shades":{"--ff-primary-10":"#e8e5fe","--ff-primary-50":"#cec8fd","--ff-primary-100":"#c0b8fd","--ff-primary-200":"#b1a6fd","--ff-primary-300":"#a092fc","--ff-primary-400":"#8d7bfc","--ff-primary-500":"#785efb","--ff-primary-600":"#5d33fb","--ff-primary-700":"#5830ee","--ff-primary-800":"#532ee1","--ff-primary-900":"#4e2bd2","--ff-primary-950":"#4828c2"},"originTime":1724853621637,"originId":"3400a0d5ced53fc5e809d7fe8b67884095697122e21aba169772a1c91b934b24"}

formFacade.langtext = {"locale":"US","language":"en"}

formFacade.load("#ff-compose");