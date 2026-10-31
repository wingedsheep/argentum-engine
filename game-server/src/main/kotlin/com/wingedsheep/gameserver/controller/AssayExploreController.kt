package com.wingedsheep.gameserver.controller

import com.wingedsheep.assay.explore.ExploreApi
import com.wingedsheep.gameserver.coverage.AiReadingService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The Argentum Assay explorer, mounted inside the app so the Set Completion view can frame it as a
 * tab beside the coverage grid.
 *
 * ## Why the page is embedded rather than reimplemented
 *
 * `just assay-explore` already serves this page from `:oracle-assay` against the **live grammar**,
 * and that liveness is the whole point of the tool: a rule you just edited is one restart away from
 * being re-measured. A React port inside `web-client` would be a second implementation of the same
 * views, drifting from the gates it displays — precisely what the module's "it is a view, never a
 * second source of truth" rule exists to prevent. So this serves the same resource and the same
 * handlers ([ExploreApi]) under a prefix, and the client frames it. Nothing here decides anything;
 * if a `when` on a route name appears in this file, it belongs in [ExploreApi].
 *
 * ## Not gated, and what that costs
 *
 * This is mounted on every server, unlike the custom-card sandbox next door. The tool is a *read*
 * over public card text — it holds no state a request can mutate, touches no game, no account and no
 * corpus — so there is nothing here to protect; the reason to think twice is resources, not exposure.
 *
 * Two of them, and [ExploreApi]'s existing behaviour is what makes them tolerable:
 *
 * - **The corpus.** The explorer needs Scryfall's Oracle bulk (~24 MB) out of `~/.cache/scryfall`,
 *   which a production container has no copy of, so the first sweep there downloads it. That is why
 *   the sweep runs on the **first request** rather than at boot: a server nobody opens the tool on
 *   never fetches anything, and the page renders immediately while `status` reports progress. A
 *   sweep that cannot run at all (no cache, no network) leaves the page up with its live parser and
 *   rule tree working and the failure reported — it does not take the endpoint down.
 * - **The goldens.** The differential decodes `mtg-sets` *test* resources, which are not in the
 *   bootJar. In production that page answers with its "no goldens found" message rather than an
 *   error; the rest of the explorer is unaffected.
 *
 * ## The AI reading panel
 *
 * `:oracle-assay` cannot see the engine AI, so this is where the explorer gets it: the page's
 * card view asks the `ai` route what the AI's static readers understand of a card, and
 * [AiReadingService] answers from `:ai`. Standalone `just assay-explore` has no provider and its
 * panel says the reading needs the game server.
 *
 *  - GET  /api/assay/explorer          → the page, with its API base rewritten to this prefix
 *  - GET  /api/assay/explorer/{route}  → the read routes (status, overview, cards, declines, …)
 *  - POST /api/assay/explorer/{route}  → the measuring routes (parse, probe)
 */
@RestController
@RequestMapping("/api/assay/explorer")
class AssayExploreController(private val aiReading: AiReadingService) {

    /**
     * Built on first use. `lazy` rather than a bean because constructing it starts a corpus sweep,
     * and a boot that pays for one nobody asked for is the difference between this being free and
     * this being a tax on every server that mounts it.
     */
    private val api: ExploreApi by lazy { ExploreApi(aiReading = aiReading).also { it.startSweep() } }

    @GetMapping(produces = [MediaType.TEXT_HTML_VALUE])
    fun page(): ResponseEntity<ByteArray> = ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        // Nothing here is cacheable: the point of the live tool is that a restart re-measures
        // against an edited grammar, and a browser holding yesterday's payload would undo that.
        .header("Cache-Control", "no-store")
        .body(api.page(API_BASE))

    @GetMapping("/{route}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(
        @PathVariable route: String,
        @RequestParam params: Map<String, String>,
    ): ResponseEntity<String> {
        if (route !in api.getRoutes) return notFound()
        return json(api.encode { api.get(route) { name -> params[name] } })
    }

    @PostMapping("/{route}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun post(
        @PathVariable route: String,
        @RequestBody(required = false) body: String?,
    ): ResponseEntity<String> {
        if (route !in api.postRoutes) return notFound()
        return json(api.encode { api.post(route, body) })
    }

    /**
     * An unknown route 404s rather than returning [ExploreApi]'s error payload. The payload exists
     * so the *page* can show a readable message for a route it asked for; a request that reaches
     * here with a name no route has is someone probing the prefix, and a 404 is the honest answer.
     */
    private fun notFound(): ResponseEntity<String> = ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    private fun json(payload: String): ResponseEntity<String> = ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header("Cache-Control", "no-store")
        .body(payload)

    private companion object {
        /**
         * What the page prepends to a route name. Must match this controller's own mapping and end
         * in a slash — [ExploreApi.page] requires the slash because the page concatenates.
         */
        const val API_BASE = "/api/assay/explorer/"
    }
}
