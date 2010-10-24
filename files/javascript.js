$(document).ready(function (){
  var options = {
    clearForm: true,
    target: '#formresult',
    success: done
  }
  $('#myform').ajaxForm(options);
  
  function  done (responseText, statusText, xhr, $form) {
    $('li').click(function() {
      var item = $(this);
      $.ajax({
        url: '/delete/'+$(this).html(),
        success: function(data) {
          item.fadeOut("slow");
        }
      });

    });  
  }
});
