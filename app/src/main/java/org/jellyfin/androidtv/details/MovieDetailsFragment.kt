package org.jellyfin.androidtv.details

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.details.actions.*
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.presentation.InfoCardPresenter
import org.jellyfin.androidtv.util.addIfNotEmpty
import org.jellyfin.androidtv.util.apiclient.getLocalTrailers
import org.jellyfin.androidtv.util.apiclient.getSimilarItems
import org.jellyfin.androidtv.util.apiclient.getSpecialFeatures
import org.jellyfin.androidtv.util.dp

class MovieDetailsFragment(private val movie: Movie) : BaseDetailsFragment<Movie>(movie) {
	// Action definitions
	private val resumeAction by lazy { ResumeAction(context!!, movie) }
	private val playAction by lazy { PlayFromBeginningAction(context!!, movie) }
	private val toggleWatchedAction by lazy { ToggleWatchedAction(context!!, movie) }
	private val toggleFavoriteAction by lazy { ToggleFavoriteAction(context!!, movie) }
	private val deleteAction by lazy { DeleteAction(context!!, movie) { activity?.finish() } }

	// Row definitions
	private val detailRow by lazy { DetailsOverviewRow(movie, actionAdapter) }
	private val chaptersRow by lazy { createListRow("Chapters", movie.chapters, ChapterInfoPresenter(context!!)) }
	private val specialsRow by lazy { createListRow("Specials", emptyList(), ItemPresenter(context!!, 250.dp, 140.dp, false)) }
	private val castRow by lazy { createListRow("Cast/Crew", movie.cast, PersonPresenter(context!!)) }
	private val relatedItemsRow by lazy { createListRow("Similar", emptyList(), ItemPresenter(context!!, 100.dp, 150.dp, false)) }
	private val trailersRow by lazy { createListRow("Trailers", emptyList(), ItemPresenter(context!!, 250.dp, 140.dp, false)) }
	private val streamInfoRow by lazy { createListRow("Media info", movie.mediaInfo.streams, InfoCardPresenter()) }

	override suspend fun onCreateAdapters(rowSelector: ClassPresenterSelector, rowAdapter: ArrayObjectAdapter, actionAdapter: ActionAdapter) {
		super.onCreateAdapters(rowSelector, rowAdapter, actionAdapter)

		// Retrieve additional info
		loadAdditionalInformation()

		// Add actions
		actionAdapter.apply {
			add(resumeAction)
			add(playAction)
			add(toggleWatchedAction)
			add(toggleFavoriteAction)

			// "More" button
			add(SecondariesPopupAction(context!!).apply {
				add(deleteAction)
			})
		}

		// Add rows
		rowAdapter.apply {
			add(detailRow)
			addIfNotEmpty(chaptersRow)
			addIfNotEmpty(specialsRow)
			addIfNotEmpty(castRow)
			addIfNotEmpty(relatedItemsRow)
			addIfNotEmpty(trailersRow)
			addIfNotEmpty(streamInfoRow)
		}
	}

	private suspend fun loadAdditionalInformation() = withContext(Dispatchers.IO) {
		// Get additional information asynchronously
		awaitAll(
			async {
				val specials = TvApp.getApplication().apiClient.getSpecialFeatures(movie).orEmpty()
				(specialsRow.adapter as ArrayObjectAdapter).apply { specials.forEach(::add) }
			},
			async {
				//todo filter on server side?
				val relatedItems = TvApp.getApplication().apiClient.getSimilarItems(movie).orEmpty().filterIsInstance<Movie>()
				(relatedItemsRow.adapter as ArrayObjectAdapter).apply { relatedItems.forEach(::add) }
			},
			async {
				val trailers = TvApp.getApplication().apiClient.getLocalTrailers(movie).orEmpty()
				(trailersRow.adapter as ArrayObjectAdapter).apply { trailers.forEach(::add) }
			}
		)
	}

	// Temporary manual action updating
//	private fun onItemChange() {
//		resumeAction.isVisible = movie.canResume
//		toggleWatchedAction.active = movie.played
//		toggleFavoriteAction.active = movie.favorite
//		deleteAction.isVisible = TvApp.getApplication().currentUser.policy.enableContentDeletion
//	}

//	override fun onResume() {
//		super.onResume()
//
//		movie.addChangeListener(::onItemChange)
//	}
//
//	override fun onPause() {
//		super.onPause()
//
//		movie.removeChangeListener(::onItemChange)
//	}
}
