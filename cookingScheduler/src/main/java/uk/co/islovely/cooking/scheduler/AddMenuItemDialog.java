//package uk.co.islovely.cooking.scheduler;
//
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.content.DialogInterface;
//import android.os.Bundle;
//import android.support.v4.app.DialogFragment;
//import android.view.LayoutInflater;
//
//public class AddMenuItemDialog extends DialogFragment {
//	@Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//	    // Get the layout inflater
//	    LayoutInflater inflater = getActivity().getLayoutInflater();
//
//	    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                // add the menu item
//            	MainActivity.menuItems.add(new MenuItem("Clicked : "+MainActivity.clickCounter++));
//            }
//	    };
//	    
//	    // Inflate and set the layout for the dialog
//	    // Pass null as the parent view because its going in the dialog layout
//	    builder.setView(inflater.inflate(R.layout.add_item_dialog, null))
//	    // Add action buttons
//	           .setPositiveButton(R.string.add_item, listener)
//	           .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//	               public void onClick(DialogInterface dialog, int id) {
//	                   //NoticeDialog.this.getDialog().cancel();
//	               }
//	           });      
//	    return builder.create();
//    }
//}
