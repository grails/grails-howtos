$('#all_links_toggler').live('click', function() {
   var allLinks = $('#all_links');
   if(allLinks.is(':visible')) {
       allLinks.hide();
       $(this).removeClass('shown');
   } else {
       allLinks.show();
       $(this).addClass('shown');
   }
});