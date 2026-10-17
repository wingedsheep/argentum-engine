import {readFile,writeFile,mkdir,copyFile,readdir} from 'node:fs/promises';
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
const source='build/compileSync/wasmJs/main/productionExecutable/kotlin',out=process.argv[2]||'dist';
await mkdir(out,{recursive:true});
const files=(await readdir(source)).filter(name=>/\.(?:mjs|wasm)$/.test(name));
for(const file of files)await copyFile(`${source}/${file}`,`${out}/${file}`);
// Enable only the browser features emitted by Kotlin. Enabling experimental stringref
// lets Binaryen introduce stringview instructions that supported browsers cannot execute.
try{
 execFileSync('wasm-opt',['--version'],{stdio:'ignore'});
 execFileSync('wasm-opt',['-Oz','--enable-gc','--enable-reference-types','--enable-exception-handling','--enable-multivalue','--enable-bulk-memory','--enable-sign-ext','--enable-nontrapping-float-to-int','--enable-mutable-globals','--strip-debug',`${out}/assay-oracle.wasm`,'-o',`${out}/assay-oracle.wasm`],{stdio:'inherit',env:{...process.env,BINARYEN_CORES:process.env.BINARYEN_CORES||'2'}});
}catch(error){if(error.code!=='ENOENT')throw error;}
await copyFile('../../LICENSE',`${out}/LICENSE.txt`);
const sha256=bytes=>createHash('sha256').update(bytes).digest('hex');
const manifest={repository:'https://github.com/wingedsheep/argentum-engine',revision:execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),license:'MIT',files:{}};
for(const file of [...files,'LICENSE.txt'])manifest.files[file]=sha256(await readFile(`${out}/${file}`));
await writeFile(`${out}/manifest.json`,JSON.stringify(manifest,null,2)+'\n');
console.log(`Packaged standalone browser checker in ${out}`);
