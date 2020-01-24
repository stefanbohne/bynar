# -*- coding: utf-8 -*-

import re

from pygments.lexer import RegexLexer, bygroups, words
from pygments.token import Text, Comment, Operator, Keyword, Name, String, \
    Number, Punctuation

__name__ = 'versailleslexer'


class VersaillesLexer(RegexLexer):
    name = 'versailles'
    filenames = ['*.vrs']
    aliases = ['vrs', 'versailles']
    mimetypes = ['text/x-vrssrc']

    flags = re.MULTILINE | re.UNICODE

    tokens = {
        'root': [
            (r'\n', Text),
            (r'\s+', Text),
            (r'\\\n', Text),  # line continuations
            (r'//(.*?)\n', Comment.Single),
            (r'/(\\\n)?[*](.|\n)*?[*](\\\n)?/', Comment.Multiline),
            (r'(import|module)\b', Keyword.Namespace),
            (r'(case|let|try|def|type|letdef|lettype|variant|forget|remember)\b',
             Keyword.Declaration),
            (words((
                'and', 'asserting', 'call', 'div', 'do', 'else', 
                'fail', 'for', 'from', 'if', 'iff', 'implies', 'in',  
                'loop', 'mod', 'or', 'pass', 'return', 'switch', 
                'unless', 'until', 'then', 'versailles', 'where', 'yield'), 
                suffix=r'\b'),
             Keyword),
            (r'(true|false|cons|nil|muladd|divmod|if|mod)\b', Name.Builtin),
            (r'(Number|Integer|Float|String|Boolean|List|Dictionary|Type|Janus_class)\b', Name.Builtin),
            (words((
                'algebraic', 'inductive', 'tuple'), suffix=r'\b'),
             Keyword.Type),
            # -- octal_lit
            (r'0[oO][0-7_]+(\.[\d_]+[pP][+\-]?[0-7_]+|'
             r'0[oO]\.[0-7_]*|[pP][+\-]?[0-7_]+)?', Number.Oct),
            # -- hex_lit
            (r'0[xX][0-9A-Fa-f_]+(\.[\d_]+[pP][+\-]?[0-9A-Fa-f_]+|'
             r'0[xX]\.[0-9A-Fa-f_]*|[pP][+\-]?[0-9A-Fa-f_]+)?', Number.Hex),
            # -- binary_lit
            (r'0[bB][0-1_]+(\.[\d_]+[pP][+\-]?[0-1_]+|'
             r'0[bB]\.[0-1_]*|[pP][+\-]?[0-1_]+)?', Number.Bin),
            # float_lit
            (r'[0-9_]+(\.[\d_]+[eEpP][+\-]?[0-9_]+|'
             r'\.[0-9_]*|[eE][+\-]?[0-9_]+)?', Number.Float),
            (r'0[dD][0-9_]+(\.[\d_]+[eEpP][+\-]?[0-9_]+|'
             r'0[dD]\.[0-9_]*|[pP][+\-]?[0-9_]+)?', Number.Float),
            # StringLiteral
            (r'"[^"]*"', String),
            # -- interpreted_string_lit
            (r'\'(\\\\|\\\'|[^\'])*\'', String),
            # Tokens
            (r'(&&|\|\||==>|<=>|==|!=|<=|>=|<|>|'
             r'<>-<>|<>->|<-<>|>-<|>->|<-<|=>|->|>-|-->|[+\-*/!~])', Operator),
            (r'(::|[|!\(\)\[\]\{\}\.,;:=\?])', Punctuation),
            # identifier
            (r'[^\W\d]\w*', Name.Other),
            (r'`[^`]*`', Name.Other),
        ]
    }