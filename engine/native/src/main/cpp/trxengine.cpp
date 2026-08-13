#include <jni.h>
#include <string>
#include <sstream>
#include <vector>
#include <cctype>
#include <cmath>

struct Piece { char c=' '; int side=0; };
static bool inb(int f,int r){return f>=0&&f<8&&r>=0&&r<8;}
static std::string sq(int f,int r){std::string s; s.push_back(char('a'+f)); s.push_back(char('1'+r)); return s;}

extern "C" JNIEXPORT jstring JNICALL
Java_com_troxzy_trxchess_engine_nativeengine_NativeEngineBridge_nativeAnalyze(JNIEnv* env,jobject, jstring jfen, jint depth){
    const char* raw=env->GetStringUTFChars(jfen,nullptr); std::string fen(raw?raw:""); env->ReleaseStringUTFChars(jfen,raw);
    std::istringstream iss(fen); std::string boardPart, stm; iss>>boardPart>>stm; Piece b[8][8]{}; int r=7,f=0;
    for(char c:boardPart){ if(c=='/'){r--;f=0;continue;} if(std::isdigit((unsigned char)c)){f+=c-'0';continue;} if(inb(f,r)){b[f][r]={c,std::isupper((unsigned char)c)?0:1};f++;} }
    int side=stm=="b"?1:0; std::string best="0000";
    const int ndf[8]={1,2,2,1,-1,-2,-2,-1}; const int ndr[8]={2,1,-1,-2,-2,-1,1,2};
    for(int sr=0;sr<8&&best=="0000";sr++)for(int sf=0;sf<8&&best=="0000";sf++){ auto p=b[sf][sr]; if(p.c==' '||p.side!=side)continue; char pc=std::tolower((unsigned char)p.c);
      if(pc=='p'){int d=side?-1:1;int nr=sr+d;if(inb(sf,nr)&&b[sf][nr].c==' ')best=sq(sf,sr)+sq(sf,nr); for(int df:{-1,1}){int nf=sf+df;if(inb(nf,nr)&&b[nf][nr].c!=' '&&b[nf][nr].side!=side)best=sq(sf,sr)+sq(nf,nr);}}
      else if(pc=='n'){for(int i=0;i<8&&best=="0000";i++){int nf=sf+ndf[i],nr=sr+ndr[i];if(inb(nf,nr)&&(b[nf][nr].c==' '||b[nf][nr].side!=side))best=sq(sf,sr)+sq(nf,nr);}}
      else {int dirs[8][2]={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};int cnt=(pc=='b'?8:(pc=='r'?4:8));for(int i=0;i<cnt&&best=="0000";i++){int nf=sf+dirs[i][0],nr=sr+dirs[i][1];for(;inb(nf,nr);nf+=dirs[i][0],nr+=dirs[i][1]){if(b[nf][nr].c==' '){best=sq(sf,sr)+sq(nf,nr);break;}if(b[nf][nr].side!=side){best=sq(sf,sr)+sq(nf,nr);break;}break;}}}
      if(pc=='k'&&best=="0000"){for(int df=-1;df<=1&&best=="0000";df++)for(int dr=-1;dr<=1&&best=="0000";dr++){if(!df&&!dr)continue;int nf=sf+df,nr=sr+dr;if(inb(nf,nr)&&(b[nf][nr].c==' '||b[nf][nr].side!=side))best=sq(sf,sr)+sq(nf,nr);}}
    }
    std::ostringstream out; out<<"info depth "<<std::max(1,(int)depth)<<" nodes 1 nps 1 score cp 0 pv "<<best<<"\n"<<"bestmove "<<best<<"\n";
    return env->NewStringUTF(out.str().c_str());
}
