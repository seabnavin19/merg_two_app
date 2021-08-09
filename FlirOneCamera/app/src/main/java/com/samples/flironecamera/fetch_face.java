package com.samples.flironecamera;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class fetch_face {
    private Context context;
    private people_face_array student;

    private RequestQueue mQueue;

    fetch_face(Context context){
        mQueue = Volley.newRequestQueue(context);
        this.context= context;

    }

    public void Fetch_Faces(){
        String url="https://intech-api.herokuapp.com/api/v1/upload_data/faces/?fbclid=IwAR2YR1OE6Uiq8N5cV5k5M4aNLJfAu-K4UT4iUW3xOdPBcjR-WYXHfdIOVuw";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    ArrayList<Float> face= new ArrayList<>();
                    JSONObject jsonObject = response.getJSONObject(0);
                    String name = jsonObject.getString("name");
                    String id= jsonObject.getString("userid");
                    Double distance=jsonObject.getDouble("distance");
                    people_face_array student = new people_face_array("0",name,1,face,"");
                    Toast.makeText(context,""+student.getName(),Toast.LENGTH_LONG).show();

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(context,"men",Toast.LENGTH_LONG).show();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mQueue.add(request);

    }

    public people_face_array getStudent() {
        return student;
    }
}
