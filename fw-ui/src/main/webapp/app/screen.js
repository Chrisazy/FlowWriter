/*
   (c) Copyright 2012 Hewlett-Packard Development Company, L.P.
   Autogenerated
   */

// JSLint directive...
/*global $: false*/

(function (api) {
  'use strict';

  //framework APIs
  var fn_api = api.fn,       //general API
      def_api = api.def,     //application definition API
      view_api = api.view;   //view API

  // snap-in libs
  var tags_api = api.lib.htmlTags,
      widget_api = api.lib.widgetFactory;
  
  fn_api.trace('including screen.js');
  
  function create(view) {
    var div = tags_api.div({cls: 'textView'}).append(tags_api.h2('Enter Controller Information')),
        tf = widget_api.textField({
          label: 'Login:',
          ph: 'Enter Login',
          on: {
            keyup: function () {
              result.html(this.value());
            }
          }
        }),
        pf = widget_api.passwordField({
          label: 'Password:',
          ph: 'Enter Password',
          on: {
            keyup: function () {
              result.html(this.value());
            }
          }
        }),

        row = tags_api.div({css: {margin: 30}}).append(tf, pf);
    
    return div.append(row);
  }

  def_api.addView('screen', {
    create: create
  });
}(SKI));
        
  //function dlg(view, titleKey, text) {
  //  var dlgDiv = tags_api.div( {
  //    attr: {
  //      id: view.mkId('dlg'),
  //      title: view.lion(titleKey)
  //    }
  //  }).append(tags_api.p(text)),
  //      $dlg = dlgDiv.domFrag(),
  //      actions = {};
  //
  //  actions[view.lion('close')] = function () {
  //    $(this).dialog("close").dialog('destory');
  //  };
  //
  //  $dlg.dialog({
  //    width: 600,
  //    buttons: actions
  //  });
  //
  //}
  //
  //// Load a view
  //function load(view) {
  //  var lion = view.lion;
  //
  //
  //  view_api.setToolbar(
  //      widget_api.button({
  //        text: lion('tbDeploy'),
  //        click: function () {
  //          dlg(view, 'deployDlg', 'This is where...');
  //        }
  //      })
  //      );
  //}
  //
  //// Add the empty view
  //def_api.addView('screen', {
  //  load: load
  //});
