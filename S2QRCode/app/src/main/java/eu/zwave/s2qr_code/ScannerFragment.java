package eu.zwave.s2qr_code;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class ScannerFragment extends Fragment {
        DecoratedBarcodeView barcodeView;
        private String toast;
        BarcodeResult mBarcodeResult;



        public ScannerFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static ScannerFragment newInstance() {
            return new ScannerFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tabbed_scanning, container, false);
            barcodeView = (DecoratedBarcodeView)rootView.findViewById(R.id.barcode_view);
            barcodeView.setStatusText("Scan S2 Code");
            barcodeView.decodeSingle(new BarcodeCallback() {
                @Override
                public void barcodeResult(BarcodeResult result) {
                    toast=result.getText();
                    mBarcodeResult=result;
                    displayToast();
                    sendData();
                }

                @Override
                public void possibleResultPoints(List<ResultPoint> resultPoints) {

                }
            });
            return rootView;
        }


        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
            if(barcodeView != null) {
                if (isVisibleToUser) {
                    barcodeView.resume();
                } else {
                    barcodeView.pauseAndWait();
                }
            }
        }

    private void sendData()
    {
        //INTENT OBJ
        Intent i = new Intent(getActivity().getBaseContext(),
                MainActivity.class);

        //PACK DATA
        i.putExtra("SENDER_KEY", "ScannerFragment");
        i.putExtra("BARCODE_RESULT_Text", mBarcodeResult.getText());

        //START ACTIVITY
        getActivity().startActivity(i);
    }


        @Override
        public void onPause() {
            super.onPause();
            barcodeView.pauseAndWait();
        }

        @Override
        public void onResume() {
            super.onResume();
            barcodeView.resume();
        }


        private void displayToast() {
            if(toast != null) {
                Toast.makeText(getActivity(), toast, Toast.LENGTH_LONG).show();
                toast = null;
            }
        }

}

