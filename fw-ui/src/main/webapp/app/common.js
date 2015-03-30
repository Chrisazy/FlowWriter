/*
    (c) Copyright 2012,2013 Hewlett-Packard Development Company, L.P.

    HP SKI Framework Reference Implementation

    == Common code for use across other modules ==

    @author Simon Hunt
 */

// JSLint directive...
/*global $: false, SKI: false */

(function (api) {
    'use strict';

    var f = api.fn,
        v = api.view,
        t = api.lib.htmlTags,
        u = api.lib.miscUtils,
        wgt = api.lib.widgetFactory,
        sampleTab,
        docTab;

    f.trace('including common.js');

    f.attachCommon('sampleTab', function () {
        return {
            label: 'Sample',
            icon: 'script'
        };
    });
    f.attachCommon('docTab', function () {
        return {
            label: 'Documentation',
            icon: 'doc'
        };
    });

    //=== Convenience methods for formatting simple constructs

    function sup(x) {
        return t.span({cls: 'super'}).append(x);
    }

    function topPaddedDiv() {
        return t.div({css: {'padding-top': 10}});
    }

    function enableToggle(initEnabled, cb) {
        var enable = initEnabled;

        function toggleText() {
            return enable ? 'Disable' : 'Enable';
        }

        return wgt.button({
            text: toggleText(),
            click: function () {
                enable = !enable;
                this.text(toggleText());
                cb(enable);
            }
        });
    }
    f.attachCommon('enableToggle', enableToggle);

    //=== Creates and returns a div with a show/hide button, and a block of
    //    source code.
    function makeCodeBlock(fileName) {
        var codeVisible = false,
            pre= t.pre({cls: 'code-block'});

        $.get(fileName, function(data) {
            pre.append(data);
        }, "text");

        pre.domFrag().hide();

        return t.div().append(topPaddedDiv().append(wgt.button({
            text: 'View Source',
            click: function () {
                pre.domFrag().slideToggle();
                codeVisible = !codeVisible;
                this.text(codeVisible ? 'Hide Source' : 'View Source');
            }
        })), pre);
    }
    f.attachCommon('makeCodeBlock', makeCodeBlock);

    // returns an array of lines from an RE match, or null if no match
    function linesFromMatch(match) {
        return match ? match[0].split('\n') : null;
    }

    // pulls out the method name from the '@api which method' doc line
    function apiMethodNameFromLines(lines) {
        var name = null,
            re = new RegExp('@api (\\w+) (.*)'),
            m;
        $.safeEach(lines, function (i, line) {
            m = line.match(re);
            if (m) {
                name = m[2];
                return false; // break
            }
        });
        return name;
    }

    function formatApiDoc(docSrc, apiSections) {
        var div = t.div({cls: 'textView'});

        function apiRegExp(id) {
            var tag = '@api ' + id;
            return new RegExp('/\\*\\*\\s+' + tag + '(.|[\\r\\n])*?\\*/', 'g');
        }

        function parseDocComment(lines) {
            var dc = {},
                desc = [],
                params = [],
                retValue,
                someReq = false,
                ul;

            // process lines prefixed with spaces-star-space ("  * ")
            $.safeEach(lines, function (i, line) {
                var m = line.match(/\s+\*\s(.*)/),
                    txt,
                    m2,
                    m3,
                    pname,
                    type,
                    rem,
                    def,
                    req;

                if (m) {
                    txt = m[1];
                    if (txt.match(/\s*@param\s+.*/)) {
                        m2 = txt.match(/\s*@param\s+(\w+)\s+\((.*?)\)\s+(.*)/);

                        if (!m2 || m2.length < 3) {
                            throw new Error('Unable to parse: "' + txt +
                                '", use form: @param p1 (type) [*]' +
                                ', @param p2 (type) [defVal]' +
                                ', @param p3 (type) param description');
                        }

                        pname = m2[1];
                        type = m2[2];
                        rem = m2[3];

                        m3 = rem.match(/\[(.*)\]\s+(.*)/);
                        if (m3) {
                            def = m3[1]; // default value
                            rem = m3[2]; // what's left
                            if (def === '*') {
                                // signifies required parameter
                                req = true;
                                def = undefined;
                            }
                        }
                        params.push({
                            pname: pname,
                            type: type,
                            req: req,
                            def: def,
                            txt: rem
                        });
                        someReq = someReq || req; // bubble up

                    } else if (txt.match(/\s*@return\s+.*/)) {
                        m2 = txt.match(/\s*@return\s+(.*)/);
                        retValue = m2[1];

                    } else {
                        desc.push(txt + '\n');
                    }
                }
            });

            dc.desc = desc.join(''); // convert to single HTML string
            dc.summary = desc.length ? desc[0] : '';
            if (someReq) {
                dc.req = t.h5('** Required');
            }
            if (retValue) {
                dc.ret = t.p({cls: 'retval'}).html(
                    t.i('Returns: '),
                    retValue
                );
            }
            if (params.length) {
                ul = t.ul();
                dc.params = ul;
                $.safeEach(params, function (i, p) {
                    var req = p.req ? sup('**') : '',
                        li = t.li({cls: 'oneliner'}).html(
                            p.pname,
                            req,
                            ' ',
                            t.i().append('(', p.type, ')'),
                            ' &ndash; ',
                            p.txt
                        );
                    if (p.def) {
                        li.append(
                            t.i(' (default value is: ')
                                .append(t.b(p.def)).append(')')
                        );
                    }
                    ul.append(li);
                });
            }

            return dc;
        }

        // iterate across each section
        $.safeEach(apiSections, function (i, sectionId) {
            var divText = t.div(),
                divAcc = t.div({cls: 'accordion'}),
                useAcc = false,
                spacer = function (h) {
                    return t.div({css: {height: h}})
                },
                apiRE = apiRegExp(sectionId),
                result,
                mLines,
                method,
                content;

            var m, m2, hdr, dc;

            while ( (result = apiRE.exec(docSrc)) != null) {
                mLines = linesFromMatch(result);
                method = apiMethodNameFromLines(mLines);

                m = method.match(/_(.*)_/);
                if (m) {
                    // section header
                    hdr = m[1];
                    m2 = hdr.match(/\*(.*)\*/);
                    hdr = (m2) ? t.h2(m2[1]) : t.h3(hdr);

                    divText.append(
                        hdr,
                        parseDocComment(mLines).desc,
                        spacer(10)
                    );

                } else {
                    // assume a function description that we are
                    // going to add as an accordion section.
                    dc = parseDocComment(mLines);

                    content = t.div({cls: 'accordion-content'});
                    content.append(
                        dc.params,
                        dc.req,
                        dc.ret,
                        dc.desc
                    );
                    divAcc.append(
                        t.h3().append(method, t.i(' - ' + dc.summary)),
                        content
                    );
                    useAcc = true;
                }

            }

            if (! mLines || ! mLines.length) {
                f.trace('Unable to parse document source for section: "' +
                        sectionId + '". Check for single line comment start: ' +
                        '"/** @api tableMethod ...".');
            }

            div.append(
                divText,
                useAcc ? divAcc : '',
                spacer(20)
            );
        });
        return div;
    }

    function loadApiDoc(view, ctx, sub) {
        var keystr = sub || '',
            iPath = view.data.infoPath,
            infoPath = iPath ? 'data/info/' + iPath : '',
            srcPath = view.data.srcPath || infoPath,
            sections = view.data.apiSections || [],
            loading = 'Loading API doc from ' + srcPath;

        f.traceFn('loadApiDoc() v=' + view.vid + ' s=' + sub);
        f.trace(loading);
        f.trace(' sections: ' + u.valueString(sections));
        v.setContent(loading + '...');
        $.get(srcPath, null, function (docSrc) {
            v.setContent(formatApiDoc(docSrc, sections));
            var $accs = $('#' + view.vid + ' .accordion');
            $accs.accordion();
            /*
             * Here's the clever bit. The subcontext (sub) if provided should
             * tell us which accordion section(s) to have active. The sub
             * value should be a bar (|) delimited string (no spaces allowed)
             * with each field providing a string to match the beginning of
             * the text in the header of each successive accordion.
             *
             * For example, if sub is "foo||baz", the required matches are:
             * 0: "foo"
             * 1: no match (defaults to section 0 active)
             * 2: "baz"
             *
             * So the 0'th (first) accordion in the view will activate the
             * first section with "foo" at the beginning of its header text
             * (or will default to the first section if no match is found).
             * Likewise with the third accordion in the view and "baz" prefix.
             */
            var keys = keystr.split('|');
            $accs.each(function (i) {
                var match = keys[i],
                    $acc = $(this),
                    aidx = 0;
                if (match) {
                    $acc.find('h3').each(function (j) {
                        var txt = $(this).text();
                        if (txt.indexOf(match) === 0) {
                            aidx = j;
                            return false;
                        }
                    });
                    $acc.accordion('option', 'active', aidx);
                }
            });

        }, 'text');
    }
    f.attachCommon('loadApiDoc', loadApiDoc);

}(SKI));
