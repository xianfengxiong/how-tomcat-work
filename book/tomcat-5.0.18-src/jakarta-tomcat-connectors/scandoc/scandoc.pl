#!/usr/bin/perl
#
# ScanDoc - Version 0.14,  A C/C++ Embedded Documentation Analyser
# ----------------------------------------------------------------
#
# Distributed under the "Artistic License".  See the file 
# "COPYING" that accompanies the ScanDoc distribution.
#
# See http://scandoc.sourceforge.net/ for more information and
# complete documentation.
#
# (c) 1997 - 2000 Talin and others.

require "ctime.pl";
require "getopts.pl";

# 1 = on (verbose); 0 = off 
$debug = 0;

# Get the current date
$date = &ctime(time);

# Set the default tab size
$tabSize = 4;

$minorVersion = 14;
$majorVersion = 0;
$scandocURL   = "http://scandoc.sourceforge.net/";

# Set up default templates
&Getopts( 'i:d:p:t:' );

if ($#ARGV < 0) {
  die "Usage: -i <doc-template> -p <output-path> -t<tabsize> -d<sym>=<value> [ <input-files> ... ]\n";
}

# Read the template
if (!defined $opt_i) {
  $opt_i = "default.pl";
}
&readTemplate( $opt_i );

# Set the destination path.
$destPath = "";
$destPath = $opt_p if (defined($opt_p));

# Set the tab size.
$tabSize = $opt_t if (defined($opt_t));

# Handle defines
if ($opt_d) {
  foreach $def (split( /,/, $opt_d )) {
    if ($def =~ /\s*(\w*)\=(.*)/) {
      $${1} = $2;
    }
    else {
      $${1} = 1;
    }
  }
}

# For each input filename, parse it
while ($srcfile = shift(@ARGV)) {

  $linenumber = 0;
  open( FILE, $srcfile ) || die "Can't open file $srcfile\n";
  print STDERR "Reading \"$srcfile\"\n";
  
  $docTag = 'description';
  $docEmpty = 1;
  $packageName = '.general';
  $author = '';
  $version = '';
  $class = 0;
  $_ = '';
  
  while (&parseDeclaration( '' )) {}
}

# Collate subclasses and associate with class record.
foreach $className (keys %subclasses) {
  my $class = &classRecord( $className );
  
  if ($class) {
    my @subs = ();
    # print STDERR "$className ", join( ',', @{$subclasses{ $className }} ), "\n";
    foreach $subName ($subclasses{ $className }) {
      if (&classRecord( $subName )) {
	push @subs, $subName;
      }
      $class->{ 'subs' } = @subs;
    }
  }
}

# Turn packages into objects. Special case for "default" package.
foreach $pkg (keys %packages)
{
  # print STDERR $pkg, "\n";
  bless $packages{ $pkg }, PackageRecord;
  if ($pkg eq '.general') {
    $packages{ $pkg }{ 'name' } = "General";
  }
  else {
    $packages{ $pkg }{ 'name' } = $pkg;
  }
  # print STDERR $packages{ $pkg }->Classes(), "\n";
}

# Execute template file
# print STDERR $docTemplate; # For debugging
eval $docTemplate;
print STDERR $@;

exit;

# ======================= Subroutines ================================

# Read a line of input, and remove blank lines and preprocessor directives.
sub rdln {
  my ($skip_next_line) = 0;
  if (defined ($_)) {
    my ($previous_line) = $_;
    while ( (/^(\s*|\#.*)$/ || $skip_next_line ) && ($_ = <FILE>)) {
      if ($previous_line =~ m/\\\s*/) { $skip_next_line = 1; }
      else { $skip_next_line = 0; }
      $previous_line = $_;
      $linenumber++; 
      if ($debug) { print STDERR "(0:$srcfile) $linenumber.\n"; } 
    }
  }
}

# Don't skip "#"
sub rdln2 {
  if (defined ($_)) {
    while (/^(\s*)$/ && ($_ = <FILE>)) {$linenumber++; if ($debug) { print STDERR "(0:$srcfile) $linenumber.\n"; } }
  }
}

# Remove comments from current line
sub removeComment {
  s|//.*||;
}

# parsing functions
sub matchKW		{ &rdln; return (s/^\s*($_[0])//, $1) if defined ($_); return (0, 0); }
#sub matchStruct		{ &rdln; return (s/^\s*(struct|class)//, $1) if defined ($_); return (0, 0); }
#sub matchPermission	{ &rdln; return (s/^\s*(public|private|protected)// && $1) if defined ($_); return (0,0); }
sub matchID		{ &rdln; return (s/^\s*([A-Za-z_]\w*)//, $1) if defined ($_); return (0,0); }
sub matchColon		{ &rdln; return (s/^\s*\://) if defined ($_); return 0; }
sub matchComma		{ &rdln; return (s/^\s*\,//) if defined ($_); return 0; }
sub matchSemi		{ &rdln; return (s/^\s*\;//) if defined ($_); return 0; }
sub matchRBracket	{ &rdln; return (s/^\s*\{//) if defined ($_); return 0; }
sub matchLBracket	{ &rdln; return (s/^\s*\}//) if defined ($_); return 0; }
sub matchRParen		{ &rdln; return (s/^\s*\(//) if defined ($_); return 0; }
sub matchLParen		{ &rdln; return (s/^\s*\)//) if defined ($_); return 0; }
sub matchRAngle		{ &rdln; return (s/^\s*\<//) if defined ($_); return 0; }
sub matchLAngle		{ &rdln; return (s/^\s*\>//) if defined ($_); return 0; }
sub matchDecl           { &rdln; return (s/^(\s*[\s\w\*\[\]\~\&\n\:]+)//, $1) if defined ($_); return (0, 0); }
sub matchOper		{ &rdln; return (s/^\s*([\~\&\^\>\<\=\!\%\*\+\-\/\|\w]*)// && $1) if defined ($_); return 0; }
sub matchFuncOper	{ &rdln; return (s/^\s*(\(\))// && $1) if defined ($_); return 0; }
sub matchAny		{ &rdln; return (s/^\s*(\S+)//, $1) if defined ($_); return (0, 0); }
sub matchChar		{ &rdln; return (s/^(.)//, $1) if defined ($_); return (0, 0); }
sub matchChar2	        { &rdln2; return (s/^(.)//, $1) if defined ($_); return (0, 0); }
sub matchString 	{ &rdln; return (s/^\"(([^\\\"]|(\\.)))*\"//, $1) if defined ($_); return (0, 0); }

# Skip to next semicolon
sub skipToSemi {
  
  while (!&matchSemi) {
    
    &rdln;
    s|//.*||;			# Eat comments
      if (&matchLBracket) {
	&skipBody;
	next;
      }
    last if !s/^\s*([^\s\{\;]+)//;
    # print STDERR "$1 ";
  }
}

# Skip function body
sub skipBody {
  local( $nest );
  
  $nest = 1;
  
  for (;;) {
    if (&matchRBracket) { $nest++; }
    elsif (&matchLBracket) {
      $nest--;
      last if !$nest;
    }
    else { 
      last if ((($valid,) = &matchKW( "[^\{\}]")) && !$valid);
    }
  }
}

# Skip a string. (multiline)
sub skipString {
  local( $char, $lastchar);
  $lastchar = "\"";
  
  for (;;) {
    ($valid, $char) = &matchChar2;
    if (($char eq "\"") && ($lastchar ne "\\")) { last; }
    if ($lastchar eq "\\") { $lastchar = " "; }
    else { $lastchar = $char; }
  }
}


# Skip everything in parenthesis.
sub skipParenBody {
  local( $nest );
  
  $nest = 1;
  
  for (;;) {
    if (&matchRParen) { $nest++; }
    elsif (&matchLParen) {
      $nest--;
      last if !$nest;
    }
    else { 
      last if ((($valid,) = &matchKW( "[^\(\)]")) && !$valid);
    }
  }
}

# Parse (*name) syntax
sub parseParenPointer {

  $parenPointerFunction = "";

  if (s/^(\s*\(\s*\*)//) {
    $decl .= $1;
    $nest = 1;
    
    for (;;) {
      # Preserve spaces, eliminate in-line comments
      &removeComment;
      while (s/^(\s+)//) { $decl .= $1; &rdln; }
      
      if (&matchRParen) { $nest++; $decl .= "("; }
      elsif (&matchLParen) {
	$decl .= ")";
	$nest--;
	last if !$nest;
      }
      elsif ((($valid, $d) = &matchKW( "[^\(\)]*")) && $valid) { $decl .= $d; }
      else { last; }
    }
    
    # Just in case there are array braces afterwards.
    while ((($valid, $d) = &matchDecl) && $valid) { $decl .= $d; }
    $parenPointerFunction = $decl;
    $parenPointerFunction =~ s/^\s+//;	# Remove whitespace from beginning
    $parenPointerFunction =~ s/\s+$//;	# Remove whitespace from end
  }
}

# Parse template arguments
sub matchAngleArgs {
  
  if (&matchRAngle) {
    local ($args, $nest);
    
    $args = "&lt;";
    $nest = 1;
    
    for (;;) {
      if (&matchRAngle) { $nest++; $args .= "&lt;"; }
      elsif (&matchLAngle) {
	$nest--;
	$args .= "&gt;";
	last if !$nest;
      }
      elsif ((($valid, $d) = &matchChar) && $valid) { $args .= $d; }
      else { last; }
    }
    return $args;
  }
  else { return ''; }
}

# convert tabs to spaces
sub expandTabs {
  local	($text) = @_;
  local 	($n);
  
  while (($n = index($text,"\t")) >= 0) {
    substr($text, $n, 1) = " " x ($tabSize-($n % $tabSize));
  }
  
  return $text;
}

# Process a line of text from a "special" comment
sub handleCommentLine {
  local ($_) = @_;
  
  if ($docEmpty) {
    # Eliminate blank lines at the head of the doc.
    return if (/^\s*$/);
  }
  
  # First, expand tabs.
  $_ = &expandTabs( $_ );
	
  # Remove gratuitous \s*\s  (james)
  s/(^|\n)\s*\*\s/$1/g;
  
  # If it's one of the standard tags
  if (s/^\s*\@(see|package|version|author|param|return|result|exception|keywords|deffunc|defvar|heading|todo)\s*//) {
    my $tag = $1;
    $tag = 'return' if ($tag eq 'result');
    
    # for param and exception, split the param name and the text
    # seperate them with tabs.
    if ($tag eq "param" || $tag eq "exception") {
      s/^\s*(\w+)\s*(.*)/\t$1\t$2/;
    }
    elsif ($tag eq "heading") {
      # 'heading' is processed by the template, if at all.
      $_ = "\@heading\t$_";
      $tag = "description";
    }
    elsif ($tag eq 'todo') {
      if ($todolist{ $srcfile } ne '') {
	$todolist{ $srcfile } .= "\n";
      }
    }
    
    # If it's @deffunc or @defvar
    if ($tag =~ /def(.*)/) {
      
      $type = $1;
      
      # @deffunc and @defvar force a comment to be written out as if there was a
      # declaration.
      # Designed for use with macros and other constructs I can't parse.
      
      if (/(\S+)\s+(.*)$/) {
	$name = $1;
	$decl = $2;
	$dbname = &uniqueName( "$baseScope$name" );
	
	my $entry = { 'type'    => $type,
		      'name'    => $name,
		      'longname'=> $name,
		      'fullname'=> "$name$decl",
		      'scopename'=>"$baseScope$name",
		      'uname'   => $dbname,
		      'decl'    => $decl,
		      'package' => $packageName };

        bless $entry, MemberRecord;

	if ($class) {
	  $entry->{ 'class' } = "$context";
	  $class->{ 'members' }{ $dbname } = $entry;
	} 
	else {
	  $packages{ $packageName }{ 'globals' }{ $dbname } = $entry;
	}
	$docTag = 'description';
	&dumpComments( $entry );
	return;
      }
    }
    elsif ($tag eq 'package') {
      s/^\s*//;
      s/\s*$//;
      $packageName = $_;
      $docTag = 'description';
      return;
    }
    elsif ($tag eq 'author') {
      $author = $_;
      $docTag = 'description';
      return;
    }
    elsif ($tag eq 'version') {
      $version = $_;
      $docTag = 'description';
      return;
    }
    
    $docTag = $tag;
  }
  elsif (/^\s*@\w+/) {
    # any other line that begins with an @ should be inserted into the main
    # description for later expansion.
    $docTag = 'description';
  }
  
  # "To-do" lists are handled specially, and not associated with a class.
  if ($docTag eq 'todo') {
    $todolist{ $srcfile } .= $_;
    return;
  }
  
  # Append to current doc tag, regardless of whether it's a new line
  # or a continuation. Also mark this doc as non-empty.
  $docTags{ $docTag } .= $_;
  $docEmpty = 0;
  
  # @see doesn't persist.
  if ($docTag eq 'see') { $docTag = 'description'; }
  
  # print STDERR ":$_";
}

# Clear doc tag information at end of class or file
sub clearComments {
  
  $docTag = 'description';
  $docEmpty = 1;
  %docTags = ();
}

# Add doc tag information to current documented item
sub dumpComments {
  local ($hashref) = @_;
  
  if ($docEmpty == 0) {
    
    if ($author ne  '') { $hashref->{ 'author'  } = $author;  }
    if ($version ne '') { $hashref->{ 'version' } = $version; }
    $hashref->{ 'sourcefile' } = $srcfile;
    
    # Store the tags for this documentation into the global doc symbol table
    foreach $key (keys %docTags) {
      my $data = $docTags{ $key };

      $data =~ s/\s*$//;
      
      $hashref->{ $key } = $data;
    }
  }
  
  &clearComments();
}

# Generate a unique name from the given name.
sub uniqueName {
  local ($name) = @_;
  
  # Duplicate doc entries need to be distinguished, so give them a different label.
  while ($docs{ $name }) {
    if ($name =~ /-(\d+)$/) {
      $name = $` . "-" . ($1 + 1);
    }
    else { $name .= "-2"; }
  }
  
  $docs{ $name } = 1;
  return $name;
}

# Get the current class record.
sub classRecord {
  local ($className) = @_;
  local ($pkg) = $classToPackage{ $className };
  
  if ($pkg) {
    return $packages{ $pkg }{ 'classes' }{ $className };
  }
  return 0;
}

# Parse a declaration in the file
sub parseDeclaration {
  
  local ($context) = @_;
  local ($baseScope) = '';
  local ($decl);
  my ($token);
  
  if ($context) { $baseScope = $context . "::"; }
  
  &rdln;

  if (!defined ($_)) { return 0; }
  
  if (s|^\s*//\*\s+||) {
    # Special C++ comment
    &handleCommentLine( $' );
    $_ = ''; &rdln;
  }	
  elsif (s|^\s*//||) { 
    # Ordinary C++ comment
    $_ = '';
    &rdln;
  }
  elsif (s|^\s*\/\*\*\s+||) {
    # Special C comments
    
    s/\={3,}|\-{3,}|\*{3,}//;			# Eliminate banner strips
    $text = '';
    $docTag = 'description';
    
    # Special comment
    while (!/\*\//) { &handleCommentLine( $_ ); $text .= $_; $_ = <FILE>; $linenumber++; if ($debug) { print STDERR "(1) $linenumber\n."; }}
    s/\={3,}|\-{3,}|\*{3,}//;			# Eliminate banner strips
    /\*\//;
    &handleCommentLine( $` );
    $text.= $`; $_ = $';
  }
  elsif (s|^\s*\/\*||) {
    # Ordinary C comment
    $text = "";
    
    while (!/\*\//) { $text .= $_; $_ = <FILE>; $linenumber++; if ($debug) { print STDERR "(2) $linenumber\n."; }}
    /\*\//;
    $text.= $`; $_ = $';
  }
  elsif ((($valid, $tag) = &matchKW( "template")) && $valid) {
    # Template definition
    $args = &matchAngleArgs;
    &rdln;
    
    ##$tmplParams = $args; JAMES
    $result = &parseDeclaration( $context );
    ##$tmplParams = ''; JAMES
    return $result;
  }
  elsif ((($valid, $tag) = &matchKW("class|struct")) && $valid) {
    # Class or structure definition
    local ($className,$class);
    
    if ((($valid, $className) = &matchID) && $valid) {
      
      return 1 if (&matchSemi);		# Only a struct tag
      
      # A class instance
      if ((($valid,)=&matchID) && $valid) {
	&matchSemi;
	return 1;
      }
      
      my $fullName = "$baseScope$className"; ##$tmplParams"; JAMES
      # print STDERR "CLASS $fullName\n";
      
      my @bases = ();
      
      if (&matchColon) {
	
	for (;;) {
	  my $p;
	  &matchKW( "virtual" );
	  $perm = "private";
	  if ((($valid, $p) = &matchKW( "public|private|protected" )) && $valid) { $perm = $p; }
	  &matchKW( "virtual" );
	  
	  last if !(  (($valid, $base) = &matchID) && $valid  );
	  
	  push @bases, $base;
	  push @{ $subclasses{ $base } }, $fullName;
	  # print STDERR " : $perm $base\n";
	  last if !&matchComma;
	}
      }
      
      #	print STDERR "\n";
      # print STDERR "parsing class $fullName\n";

      if ($docEmpty == 0) {
	$class = { 'type'    => $tag,
		   'name'    => $fullName,
		   'longname'=> "$tag $className",
		   'fullname'=> "$tag $className",
		   'scopename'=> "$tag $fullName",
		   'uname'   => $fullName,
		   'bases'   => \@bases,
		   'package' => $packageName,
		   'members' => {} };
	
	# print STDERR "$className: @bases\n";
	
	bless $class, ClassRecord;
	
	print STDERR "   parsing class $fullName\n";
	# $classToPackage{ $className } = $packageName;
	$classToPackage{ $fullName } = $packageName;
	# $classList{ $className } = $class;
	$classList{ $fullName } = $class;
	$packages{ $packageName }{ 'classes' }{ $fullName } = $class;
	&dumpComments( $class );
      }
      
      if (&matchRBracket) {
	local ($perm) = ("private");
	
	while (!&matchLBracket) {
	  my $p;
	  if ((($valid, $p) = &matchKW( "public\:|private\:|protected\:" )) && $valid) {
	    $perm = $p;
	  }
	  else {
	    &parseDeclaration( $fullName )
	      || die "Unmatched brace! line = $linenumber\n";
	  }
	}
	
	&matchSemi;
      }
      
      &clearComments;
    }
  }
  elsif ( ((($valid,)=&matchKW( "enum")) && $valid) || ((($valid,)=&matchKW( "typedef" )) && $valid)) {
    &skipToSemi;
  }
  elsif ((($valid,)=&matchKW( "friend\s*class" )) && $valid) {
    &skipToSemi;
  }
  elsif ((($valid, $token) = &matchKW("extern\\s*\\\"C\\\"")) && $valid) {
    &matchRBracket;
    while (!&matchLBracket) {
      &parseDeclaration( '' ) || die "Unmatched brace! line = $linenumber\n";
    }
    &matchSemi;
  }
  # elsif ($kw = &matchID) {
  #   $type = "$kw ";
  #
  #   if ($kw =~/virtual|static|const|volatile/) {
  #	$type .= &typ;
  #   }
  # }
  elsif ((($valid, $decl) = &matchDecl) && $valid) {
    my ($instanceClass) = "";
    
    # print STDERR "DECLARATION=$decl, REST=$_, baseScope=$baseScope\n";

    return 1 if ($decl =~ /^\s*$/);

    if (!($class)) {
      if ($decl =~ s/(\S*\s*)(\S+)\:\:(\S+)\s*$/$1$3/) {
        $instanceClass = $2;
      }
    }

    # Eliminate in-line comments
    &removeComment;
    
    # Check for multi-line declaration
    while ((($valid, $d) = &matchDecl) && $valid) { $decl .= $d; }
    
    # Handle template args, but don't let operator overloading confuse us!
    $tempArgs = '';
    if (!($decl =~ /\boperator\b/) && ($tempArgs = &matchAngleArgs)) {
      $tempArgs = $decl . $tempArgs;
      $decl = '';
      while ((($valid, $d) = &matchDecl) && $valid) { $decl .= $d; }
    }
    
    # Look for (*name) syntax
    &parseParenPointer;
    
    # Special handling for operator... syntax
    $oper = "";
    if ($decl =~ s/\boperator\b(.*)/operator/) {
      $oper = $1;
      $oper .= &matchOper;
      # If, after all that there's no opers, then try a () operator
      if (!($oper =~ /\S/)) { $oper .= &matchFuncOper; }
    }

    ($type,$mod,$decl) = $decl =~ /([\s\w]*)([\s\*\&]+\s?)(\~?\w+(\[.*\])*)/;
    
    if ($parenPointerFunction) {
      $decl=$parenPointerFunction;
    }
    
    $type = $tempArgs . $type;
    $decl .= $oper;
    
    if ($mod =~ /\s/) { $type .= $mod; $mod = ""; }
    
    for (;;) {
      
      # print STDERR "Looping: $type/$mod/$decl\n";
      
      if (&matchRParen) {
	$nest = 1;
	$args = "";
	
	for (;;) {
	  # print STDERR "Argloop $_\n";
	  
	  # Process argument lists.
	  
	  # Preserve spaces, eliminate in-line comments
	  # REM: Change this to save inline comments and automatically
	  # generate @param clauses
	  s|//.*||;
	  while (s/^(\s+)//) { $args .= " "; &rdln; }
	  
	  if (&matchRParen) { $nest++; $args .= "("; }
	  elsif (&matchLParen) {
	    $nest--;
	    last if !$nest;
	    $args .= ")";
	  }
	  elsif ((($valid, $d) = &matchKW( "[\,\=\.\:\-]" )) && $valid) { $args .= $d; }
	  elsif ((($valid, $d) = &matchDecl) && $valid) { $args .= $d; }
	  elsif ((($valid, $d) = &matchAngleArgs) && $valid) { $args .= $d; }
	  elsif ((($valid, $d) = &matchString) && $valid) { $args .= "\"$d\""; }
	  else { last; }
	}
				
	# print STDERR "$type$mod$baseScope$decl($args);\n";
	
	&matchKW( "const" );
	
	# Search for any text within the name field
	# if ($docTag && $decl =~ /\W*(~?\w*).*/)
	if ($docEmpty == 0) {
	  $type =~ s/^\s+//;
	  $mod  =~ s/\&/\&amp;/g;
	  $args =~ s/\&/\&amp;/g;
	  $args =~ s/\s+/ /g;
	  $dbname = &uniqueName( "$baseScope$decl" );
	  
	  my $entry = { 'type'    => 'func',
			'name'    => $decl,
			'longname'=> "$decl()",
			'fullname'=> "$type$mod$decl($args)",
			'scopename'=>"$type$mod$baseScope$decl($args)",
			'uname'   => $dbname,
			'decl'    => "$type$mod$decl($args)",
			'package' => $packageName };
	  
	  bless $entry, MemberRecord;
	  
	  if ($class) {
	    $entry->{ 'class' } = "$context";
	    $class->{ 'members' }{ $dbname } = $entry;
	  }
	  elsif ($instanceClass) {
	    $class = &classRecord ($instanceClass);
	    if (!($class)) {
	      print STDERR "WARNING: Skipping \"$instanceClass\:\:$decl\".  Class \"$instanceClass\" not declared ($linenumber).\n";
	    } else {
	      $entry->{ 'class' } = "$instanceClass";
	      $class->{ 'members' }{ $dbname } = $entry;
	      $class = 0;
	    }
	  }
	  else {
	    $packages{ $packageName }{ 'globals' }{ $dbname } = $entry;
	  }
	  &dumpComments( $entry );
	}
	else { &clearComments; }
	
	s|//.*||;
	
	# Constructor super-call syntax
	if (&matchColon) {
	  
	  # Skip over it.
	  for (;;) {
	    &rdln;
	    last if /^\s*(\{|\;)/;
	    last if !((($valid,)=&matchAny) && $valid);
	  }
	}
	
	last if &matchSemi;
	if (&matchRBracket) { &skipBody; last; }
	last if !&matchComma;
	last if !((($valid, $decl) = &matchDecl) && $valid);
	
	# Look for (*name) syntax
	&parseParenPointer;
	
	$decl =~ s/^\s*//;
	$oper = "";
	if ($decl =~ /\boperator\b/) {
	  $decl =~ s/\boperator\b(.*)/operator/;
	  $oper = $1 . &matchOper;
	}
	($mod,$d) = $decl =~ /^\s*([\*\&]*)\s*(\~?\w+(\[.*\])*)/;
	$decl .= $oper;
	$decl = $d if $d ne "";
      }
      else {
	s|//.*||;
	
	$final = 0;
	
	if ((($valid,)=&matchKW( "\=" )) && $valid) {
	  for (;;) {
	    
	    if (&matchRBracket) {
	      &skipBody;
	      $final = 1;
	      last;
	    }
	    
	    if (&matchSemi) {
	      $final = 1;
	      last;
	    }
	    
	    # var = new ... (...)
	    if ((($valid,)=&matchKW("new")) && $valid) {
	      &matchKW("[A-Za-z_0-9 ]*");
	      if (&matchRParen) {
	        &skipParenBody;
	      }
	    }
	    
	    # var = (.....) ...
	    if (&matchRParen) {
	      &skipParenBody;
	    }
	    
	    # var = ... * ...
	    &matchKW ("[\/\*\-\+]*");
	    
	    # var = "..."
	    if ((($valid,) = &matchKW ("[\"]")) && $valid) {
	      &skipString;
	    }
	    #&matchString;
	    
	    last if /^\s*,/;
	    #last if !((($valid,)=&matchAny) && $valid);
	    last if !((($valid,)=&matchKW("[A-Za-z_0-9 \-]*")) && $valid);
	    if (&matchSemi) {
	        $final = 1;
	        last;
	    }
	  }
	}
	
	s|//.*||;
	
	# void ~*&foo[];
	# void foo[];
	# void far*foo[];
	# print STDERR "Decl: $type$mod$baseScope$decl;\n";

	# Search for any text within the name field
	if ($docEmpty == 0 && ($decl =~ /\W*(~?\w*).*/))
	  {
	    $mod  =~ s/\&/\&amp;/g;
	    $name = $decl;
	    
	    $dbname = &uniqueName( "$baseScope$1" );
	    
	    my $entry = { 'type'     => 'var',
			  'name'     => $1,
			  'longname' => "$name",
			  'fullname' => "$type$mod$decl",
			  'scopename'=> "$baseScope$type$mod$decl",
			  'uname'    => $dbname,
			  'decl'     => "$type$mod$decl",
			  'package'  => $packageName };
	    
	    bless $entry, MemberRecord;
	    
	    if ($class) {
	      $entry->{ 'class' } = "$context";
	      $class->{ 'members' }{ $dbname } = $entry;
	    }
	    else {
	      $packages{ $packageName }{ 'globals' }{ $dbname } = $entry;
	    }
	    &dumpComments( $entry );
	  }
	else { &clearComments; }
	
	last if $final;
	last if &matchSemi;
	last if !&matchComma;
	last if !((($valid, $decl) = &matchDecl) && $valid);
	
	# Look for (*name) syntax
	&parseParenPointer;
	
	$decl =~ s/^\s*//;
	($mod,$d) = $decl =~ /^\s*([\*\&]*)(\~?\w+(\[.*\])*)/;
	$decl = $d if $d ne "";
      }
    }
  }
  elsif ($context ne "" && /^\s*\}/) {
    # print STDERR "Popping!\n";
    return 1;
  }
  elsif (&matchRBracket) {
    &skipBody;
  }
  elsif ((($valid, $token) = &matchAny) && $valid) {
    # Comment in for debugging
    # print STDERR "token: $token \n";
  }
  else { return 0; }
  
  return 1;
}

# read a file into a string ( filename, default-value )
sub readFile {
  local ( $filename, $result ) = @_;
  
  if ($filename && open( FILE, $filename )) {
    $result = "";
    while (<FILE>) { $result .= $_; }
    close( FILE );
  }
  return $result;
}

# Read the entire document template and translate into PERL code.
sub readTemplate {
  local ( $filename ) = @_;
  $docTemplate = '';
  $indent = '';
  $literal = 1;  # We're in literal mode.
  
  if (!-e $filename) {
    if (-e "./templates/$filename") { $filename = "./templates/$filename"; }
    elsif (-e "../templates/$filename") { $filename = "../templates/$filename"; }
    else { die "Could not find template '$filename'.\n"; }
  }
  
  open( FILE, $filename ) || die "Error opening '$filename'.\n";
  while (<FILE>) {
    last if (/END/);
    
    # if we found a code entry.
    for (;;) {
      &expandTabs( $_ );
      if ($literal) {
	# Check for beginning of code block.
	if (s/^(.*)\<\<//) {
	  $line = $1; 
	  if (substr( $line, 0, length( $indent ) ) eq $indent) {
	    substr( $line, 0, length( $indent ) ) = '';
	  }
	  
	  if ($line ne '') {
	    $line =~ s/\"/\\\"/g;
	    $line =~ s/\$\((\w+)\.(\w+)\)/\" \. \$$1->$2() \. \"/g;
	    $docTemplate .= "${indent}print\"$line\";";
	  }
	  # else { $docTemplate .= "\n"; }
	  $literal = 0;
	}
	else {
	  if (substr( $_, 0, length( $indent ) ) eq $indent) {
	    substr( $_, 0, length( $indent ) ) = "";
	  }
	  chop;
	  s/\"/\\\"/g;
	  s/\$\((\w+)\.(\w+)\)/\" \. \$$1->$2() \. \"/g;
	  $_ = $indent . "print \"" . $_ . "\\n\";\n";
	  last;
	}
      }
      else {
	# Check for beginning of literal block.
	if (s/^(\s*)\>\>//) {
	  $indent = $1;
	  $literal = 1;
	}
	elsif (s/^(\s*)(.*)\>\>//) {
	  $docTemplate .= "$indent$2";
	  $literal = 1;
	}
	else {
	  last;
	}
      }
    }
    
    $docTemplate .= $_;
  }
  close( FILE );
  # print $docTemplate;
}

# Functions intended to be called from doc template file.

# Open a new output file
sub file {
  my $mfile = $_[ 0 ];
  
  open( STDOUT, ">$destPath$mfile" ) || die "Error writing to '$mfile'\n";
}

# return list of package objects
sub packages {
  my ($p, @r);
  @r = ();
  
  foreach $p (sort keys %packages) {
    push @r, $packages{ $p };
  }
  return @r;
}

# return list of source files which have to-do lists
sub todolistFiles {
  my ($p, @r);
  @r = ();
  
  foreach $p (sort keys %todolist) {
    push @r, $p;
  }
  return @r;
}

# return list of tab-delimited to-do-list texts.
sub todolistEntries {
  local $_ = $todolist{ $_[0] };
  s/^\s+//;				# Remove whitespace from beginning
  s/\s+$/\n/;				# Remove whitespace from end
  return split( /\n/, $_ );
}

# Convert package name to URL.
sub packageURL {
  my $p = $_[0];
  
  if ($p eq 'General') { $p = '.general'; }
  if ($p eq '') { $p = '.general'; }
  
  if (ref $packages{ $p }) {
    return $packages{ $p }->url();
  }
  return 0;
}

# Get the see-also list for an object
sub seealsoList {
  my $self = shift;
  my ($see, $name, $url, $p, @r);
  @r = ();
  
  if (defined ($self->{ 'see' })) {
    foreach $_ (split(/\n/,$self->{ 'see' })) {
      
      if (/^\<a\s+href/) { # if already an HREF.
	$name = $_;
	$url = 0;
      }
      elsif (/([^\#]*)\#(.*)/) { # If a package name is present
	$url = &packageURL( $1 ) . '#' . $2;
	$name = $2;
      }
      else {
	$name = $_;
	$url = "#$_";
	
	# This doesn't appear to do anything - so I commented it.  (james)
	# Look up the package in the index and use it to construct the html filename.
	#if (/^([^\:]*)\:\:(.*)/) {
	#  $className = ($1 eq '') ? '' : $classToPackage{ $1 };
	#  $p = $packageToFile{ $className };
	#  if ($p ne '') {
	#    $url = &packageURL( $1 ) . '#' . $_;
	#  }
	#}
      }
      
      $url =~ s/^\:*//;		# Remove leading colons from name
      $url =~ s/::/-/g;		# Replace :: with dash
      
      my $entry = { 'name' => $name,
		    'url'  => $url };
      
      bless $entry, DocReference;
      
      push @r, $entry;
    }
  }
  return @r;
}

# Class for parsed package
package PackageRecord;

sub classes {
  my $self = shift;
  my $classes = $self->{ 'classes' };
  return map $classes->{ $_ }, (sort keys %$classes);
}

sub globals {
  my $self = shift;
  my $globals = $self->{ 'globals' };
  return map $globals->{ $_ }, (sort keys %$globals);
}

sub globalvars {
  my $self = shift;
  my $globals = $self->{ 'globals' };
  my ($p, @r);
  @r = ();
  
  foreach $p (sort keys %$globals) {
    my $m = $globals->{ $p };
    if ($m->{ 'type' } ne 'func') { push @r, $m; }
  }
  return @r;
}

sub globalfuncs {
  my $self = shift;
  my $globals = $self->{ 'globals' };
  my ($p, @r);
  @r = ();
  
  foreach $p (sort keys %$globals) {
    my $m = $globals->{ $p };
    if ($m->{ 'type' } eq 'func') { push @r, $m; }
  }
  return @r;
}

sub name {
  my $self = shift;
  return $self->{ 'name' };
}

sub url {
  my $self = shift;
  return "default-pkg.html" if ($self->{ 'name' } eq '.general');
  return $self->{ 'name' } . '.html';
}

sub anchor {
  my $self = shift;
  my $url = $self->{ 'name' };
  return $url;
}

# Class for parsed class
package ClassRecord;

sub keywords    { return ${$_[0]}{ 'keywords' }; }
sub author      { return ${$_[0]}{ 'author' }; }
sub version     { return ${$_[0]}{ 'version' }; }
sub name        { return ${$_[0]}{ 'name' }; }
sub longname    { return ${$_[0]}{ 'longname' }; }
sub fullname    { return ${$_[0]}{ 'fullname' }; }
sub scopename   { return ${$_[0]}{ 'scopename' }; }
sub sourcefile  { return ${$_[0]}{ 'sourcefile' }; }
#sub description { return &::processDescription( ${$_[0]}{ 'description' } ); }
sub description { return ${$_[0]}{ 'description' }; }
sub seealso     { &::seealsoList( $_[0] ); }

sub url {
  my $self = shift;
  return 0 unless $self->{ 'package' };
  my $pname = ::packageURL( $self->{ 'package' } );
  my $url = $self->{ 'uname' };
  $url =~ s/::/-/g;
  return "$pname#$url";
}

sub anchor {
  my $self = shift;
  my $url = $self->{ 'uname' };
  $url =~ s/::/-/g;
  return $url;
}

sub members {
  my $self = shift;
  my $members = $self->{ 'members' };
  my ($p, @r);
  @r = ();
  
  foreach $p (sort keys %$members) {
    push @r, $members->{ $p };
  }
  return @r;
}

sub membervars {
  my $self = shift;
  my $members = $self->{ 'members' };
  my ($p, @r);
  @r = ();
  
  foreach $p (sort keys %$members) {
    my $m = $members->{ $p };
    if ($m->{ 'type' } ne 'func') { push @r, $m; }
  }
  return @r;
}

sub memberfuncs {
  my $self = shift;
  my $members = $self->{ 'members' };
  my ($p, @r);
  @r = ();
  
  foreach $p (sort keys %$members) {
    my $m = $members->{ $p };
    if ($m->{ 'type' } eq 'func') { push @r, $m; }
  }
  return @r;
}

sub baseclasses {
  my $self = shift;
  my $bases = $self->{ 'bases' };
  my ($p, $class, @r);
  @r = ();
  
  foreach $p (@$bases) {
    
    unless ($class = $::classList{ $p }) {
      # It's one we don't know about, so just make something up
      $class = { 'name'    => $p,
		 'longname'=> "class $p",
		 'fullname'=> "class $p",
		 'scopename'=>"class $p",
		 'uname'   => $p,
		 'members' => {} };
      
      if ($::classToPackage{ $p }) {
	$class->{ 'package' } = $::classToPackage{ $p };
      }
      
      bless $class, ClassRecord;
    }
    push @r, $class;
  }
  return @r;
}

sub subclasses {
  my $self = shift;
  my $subs;
  my ($p, $class, @r);
  @r = ();
  
  if (defined ($self->{ 'subs' })) {
    $subs = $self->{ 'subs' };
    foreach $p (sort @$subs) {
      $class = $::classList{ $p };
      push @r, $class;
    }
  }
  return @r;
}

# Class for parsed class member or global
package MemberRecord;

sub type         { return ${$_[0]}{ 'type' }; }
sub keywords     { return ${$_[0]}{ 'keywords' }; }
sub author       { return ${$_[0]}{ 'author' }; }
sub version      { return ${$_[0]}{ 'version' }; }
sub name         { return ${$_[0]}{ 'name' }; }
sub longname     { return ${$_[0]}{ 'longname' }; }
sub fullname     { return ${$_[0]}{ 'fullname' }; }
sub scopename    { return ${$_[0]}{ 'scopename' }; }
sub returnValue  { return ${$_[0]}{ 'return' }; }
sub sourcefile   { return ${$_[0]}{ 'sourcefile' }; }
sub description  { return ${$_[0]}{ 'description' }; }
sub seealso      { &::seealsoList( $_[0] ); }

sub url {
  my $self = shift;
  return 0 unless $self->{ 'package' };
  my $pname = ::packageURL( $self->{ 'package' } );
  my $url = $self->{ 'uname' };
  $url =~ s/::/-/g;
  return "$pname#$url";
}

sub anchor {
  my $self = shift;
  my $url = $self->{ 'uname' };
  $url =~ s/::/-/g;
  $url;
}

sub params {
  my $self = shift;
  my $params = $self->{ 'param' };
  my @r;
  @r = ();
  
  return 0 unless ($params);
  
  my @paramList = split( /\t/, $params );
  
  for ($i = 1; $i < $#paramList; $i += 2) {
    my $entry = { 'name'        => $paramList[ $i ],
		  'description' => $paramList[ $i + 1 ] };
    
    bless $entry, ArgRecord;
    
    push @r, $entry;
  }
  return @r;
}

sub exceptions {
  my $self = shift;
  my $params = $self->{ 'exception' };
  my @r;
  @r = ();
  
  return 0 unless ($params);
  
  my @paramList = split( /\t/, $params );
  
  for ($i = 1; $i < $#paramList; $i += 2) {
    my $entry = { 'name'        => $paramList[ $i ],
		  'description' => $paramList[ $i + 1 ] };
    
    bless $entry, ArgRecord;
    
    push @r, $entry;
  }
  return @r;
}

package ArgRecord;
sub name        { return ${$_[0]}{ 'name' }; }
sub description { return ${$_[0]}{ 'description' }; }

package DocReference;
sub name        { return ${$_[0]}{ 'name' }; }
sub url         { return ${$_[0]}{ 'url' }; }
