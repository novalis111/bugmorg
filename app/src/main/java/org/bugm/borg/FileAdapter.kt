package org.bugm.borg

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.bugm.borg.databinding.ItemFileBinding

class FileAdapter(private val selectedFiles: MutableList<SelectedFile>) :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemFileBinding.inflate(layoutInflater, parent, false)
        return FileViewHolder(binding, this)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val selectedFile = selectedFiles[position]
        holder.bind(selectedFile)
    }

    override fun getItemCount(): Int {
        return selectedFiles.size
    }

    fun removeItemAt(position: Int) {
        if (position in 0 until selectedFiles.size) {
            selectedFiles.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    class FileViewHolder(
        private val binding: ItemFileBinding, private val adapter: FileAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(selectedFile: SelectedFile) {
            binding.fileTextView.text = selectedFile.name

            // Add click listener for the remove button
            binding.removeButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    adapter.removeItemAt(position)
                }
            }
        }
    }
}
