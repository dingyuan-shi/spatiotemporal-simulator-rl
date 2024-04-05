import os
import sys
import re
from setuptools import setup
from distutils.extension import Extension

from Cython.Build import cythonize
from setuptools.command.build_ext import build_ext

SUB_MODULE = sys.argv[-1]
sys.argv.pop()
print(f"compiling {SUB_MODULE}==========")

class my_build_ext(build_ext):
    def build_extensions(self):
        # self.compiler.compiler_so.remove('-Wstrict-prototypes')
        super(my_build_ext, self).build_extensions()

ABS_DIR: str = os.path.dirname(os.path.abspath(__file__))

SUB_MODULE_DIR: str = os.path.join(ABS_DIR, SUB_MODULE)
LIB_DIR: str = os.path.join(SUB_MODULE_DIR, "cpplib")

# definitions
extensions = [
    Extension(SUB_MODULE, 
    [
        f"{LIB_DIR}/{SUB_MODULE}.pyx",
    ] + [os.path.join(LIB_DIR, file_name) 
            for file_name in os.listdir(LIB_DIR) if re.match(r"^raw_(.*)\.cpp$", file_name)], 
    language="c++")]
# setup function
setup(name=SUB_MODULE, ext_modules=cythonize(extensions, language_level = 3), cmdclass={'build_ext': my_build_ext})
