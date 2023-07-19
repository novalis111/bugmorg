package org.bugm.borg

import android.net.Uri
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class UriTypeAdapter : TypeAdapter<Uri>() {
    override fun write(out: JsonWriter?, value: Uri?) {
        if (value == null) {
            out?.nullValue()
            return
        }
        out?.value(value.toString())
    }

    override fun read(`in`: JsonReader?): Uri? {
        if (`in`?.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return Uri.parse(`in`?.nextString())
    }
}
