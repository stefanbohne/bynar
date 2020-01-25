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
            (r'(case|let|try|def|type|letdef|lettype|variant|forget|remember|=)\b',
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
            (r'0[oO][0-7_]+(\.[0-7_]+[pP][+\-]?[\d_]+|'
             r'0[oO]\.[0-7_]*|[pP][+\-]?[\d_]+)?', Number.Oct),
            # -- hex_lit
            (r'0[xX][0-9A-Fa-f_]+(\.[0-9A-Fa-f_]+[pP][+\-]?[\d_]+)?|'
             r'0[xX]\.[0-9A-Fa-f_]*|[pP][+\-]?[\d_]+', Number.Hex),
            # -- binary_lit
            (r'0[bB][0-1_]+(\.[0-1_]+[pP][+\-]?[\d_]+)?|'
             r'0[bB]\.[0-1_]*|[pP][+\-]?[\d_]+', Number.Bin),
            # float_lit
            (r'[0-9_]+(\.[\d_]+[eEpP][+\-]?[\d_]+)?|'
             r'\.[0-9_]*|[eE][+\-]?[\d_]+', Number.Float),
            (r'0[dD][0-9_]+(\.[\d_]+[eEpP][+\-]?[\d_]+)?|'
             r'0[dD]\.[0-9_]*|[pP][+\-]?[\d_]+', Number.Float),
            # StringLiteral
            (r'"[^"]*"', String),
            # -- interpreted_string_lit
            (r'\'(\\\\|\\\'|[^\'])*\'', String),
            # Tokens
            (r'(&&|\|\||==>|<=>|==|!=|<=|>=|<|>|'
             r'<>-<>|<>->|<-<>|>-<|>->|<-<|=>|->|>-|-->|[+\-*/!~])', Operator),
            (r'(::|[|!\(\)\[\]\{\}\.,;:=\?])', Punctuation),
            # identifier
            (r'[a-zA-Z_]\w*', Name.Other),
            (r'`[^`]*`', Name.Other),
        ]
    }