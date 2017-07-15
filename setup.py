from setuptools import setup, find_packages

setup (
  name='versailleslexer',
  packages='versailleslexer',
  entry_points =
  """
  [pygments.lexers]
  versailleslexer = versailleslexer:VersaillesLexer
  """,
  install_requires=['sphinxcontrib-inlinesyntaxhighlight'],
)