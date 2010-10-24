$(document).ready(function() {
  
  function clear_cell() {
    $('p.fix').remove();
    $('div.cnd').remove();
  }

  function clean_init_val(formData, jqForm, options) {
    var init_val = $(formData).val();
    formData[0].value = init_val.replace(/[^\d]/g,'');
  }

  //
  function done(json, statusText, xhr, $form) {
    clear_cell();
    $.each(json, function(index, value) {
      //
      if (isFinite(value)) {
        if (0 == value) value = '';
        $('#'.concat(index)).append('<p class=\'fix\'>' + value + '</p>');
      } else {
        //
        $.each(value, function(ind, val) {
          if (0 == val) val = '';
          $('#'.concat(index)).append('<div class=\'cnd\' number=\'' + val  + '\'>' + val + '</div>');
        }); 
      }
    });

    //
    //function test(json, statusText, xhr, $form) {
    //  alert(json.index + '=' + json.value);
    //}

    $('.cnd').click( function() {
       $(this).ajaxSubmit({
         target: '#base',
         url: '/cell',
         type: 'POST',
         data: {index: $(this).parent().attr('id'), value: $(this).attr('number')},
         dataType: 'json',
         success: done
       });
    }); 
  }

  // 
  $('#init').ajaxForm(
    {target: '#base',
     clearForm: true, dataType: 'json', 
     beforeSubmit: clean_init_val, 
     success: done});

  $('#hint1').ajaxForm({target: '#base', clearForm: true, dataType: 'json', success: done}); 
  $('#hint2').ajaxForm({target: '#base', clearForm: true, dataType: 'json', success: done}); 
});
