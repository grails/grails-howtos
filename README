Grails HOWTOs
=============

Grails is a modern rapid web application development framework for the JVM. It has its [own user guide] (http://grails.org/doc/latest/) that contains lots of reference material. This project _does not_ replace that. Instead, it complements the user guide by providing standalone documents that explain how to do one thing. The idea is that these HOWTO guides will be created and maintained by the community.

If you would like to contribute, simply send an internal GitHub message to 'pledbrook' requesting commit access. Once granted access, you will be able to directly commit to this repository.

Building
--------

Once you have cloned the repository locally, all you have to do is run

    ./gradlew docs

from the root of the project. This will generate all the HOWTO guides in all the languages inside the `build/docs` directory. If you would like to generate the guides only for a particular language, simply add the language suffix. For example,

    ./gradlew docs_fr

will generate the French HOWTO guides but no others.

Creating a HOWTO
----------------

All the guides are standalone gdoc files that reside under the `src/$lang` directories. To add a standard English guide, simply put it in the `src/en` directory.

The only requirements of the source file are that the first line is an `h1.` heading, which becomes the title of the HOWTO, and `h2.` is used for the top-level sections. Links between HOWTOs are not supported at this moment in time.

As soon as you create the gdoc file, it will be processed automatically by the build. The resulting HTML file has the same base name as the gdoc file.

Community
---------

This will be an open repository allowing anyone who's interested to contribute. On the flip side, there is no central editorial control, so contributors are expected to police the system themselves to prevent abuse.

Customising the styling
-----------------------

The HTML layout for the HOWTO guides is defined in the file `resources/templates/how-to-template.html`. Static resources required by the template, such as images and stylesheets, should be placed in the respective directory under `resources/images`, `resources/css` and `resources/js` as appropriate.

When adding links to static resources inside the template, be sure to use the `resourcesPath` variable like so:

     <link href="${resourcesPath}/css/main.css" type="text/css" ...>

This ensures that the template works regardless of whether the generated guide is in a language-specific directory or not.
