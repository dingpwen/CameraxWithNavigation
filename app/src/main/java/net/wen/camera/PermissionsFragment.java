package net.wen.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

/**
 * A simple {@link Fragment} subclass.
 */
public class PermissionsFragment extends Fragment {
    //private final ArrayList<String> PERMISSIONS_REQUIRED = new ArrayList<String>(
     //       Arrays.asList(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION));
    private static final String[] PERMISSIONS_REQUIRED = {
            Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int PERMISSIONS_REQUEST_CODE = 10;

    public PermissionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!hasPermissions(requireContext())){
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE);
        } else {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                    .navigate(R.id.action_permissions_to_camera);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == requestCode) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]){
                Snackbar.make(findRootView(), "Permission request granted", Snackbar.LENGTH_SHORT).show();
                Navigation.findNavController(requireActivity(), R.id.fragment_container)
                        .navigate(R.id.action_permissions_to_camera);
            } else {
                Snackbar.make(findRootView(), "Permission request denied", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private View findRootView() {
        return requireActivity().findViewById(android.R.id.content);
    }


    public static boolean hasPermissions(Context context) {
        for(String permission:PERMISSIONS_REQUIRED){
            if(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            return false;
        }
        return true;
    }
}
