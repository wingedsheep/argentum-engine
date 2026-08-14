package com.wingedsheep.engine.registry

import com.wingedsheep.engine.handlers.effects.token.TokenArt
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.mtg.sets.tokens.TokenArtData
import com.wingedsheep.mtg.sets.tokens.TokenCreationSites
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Corpus-wide gate: **every token any registered card can create resolves to an image.**
 *
 * A token entity with a null `imageUri` isn't a blank card in the client — it falls through to
 * `getScryfallFallbackUrl`, which asks `api.scryfall.com/cards/named?exact=<name>` and renders
 * whatever printing Scryfall happens to return. That is how Arahbo, the First Fang's Foundations
 * Cat ended up showing Dominaria Remastered art: nothing was *missing*, so nothing failed — the
 * art was just silently wrong. This test makes that state a build failure instead.
 *
 * Token sites come from [TokenCreationSites], which walks the serialised card tree, so a
 * `CreateToken` nested anywhere — inside a composite, a mode, a pipeline, a reflexive trigger, a
 * granted ability, a `ConvertCountersToTokens` — is still found. The resolution mirrors the
 * executors': explicit `imageUri` → the minting set's [TokenArtRegistry] entry (hand-authored
 * `MtgSet.tokenArt` ahead of synced [TokenArtData]) → the generic [TokenArt] table, or for
 * predefined tokens the canonical printing in `PredefinedTokens`.
 *
 * Fixing a failure means adding the creature type to [TokenArt.IMAGES] — or, when the art has to
 * be exactly right for that set, adding a `TokenPrinting` to the set's `MtgSet.tokenArt`.
 *
 * This is the *floor*, not the goal: passing only means nothing renders with a Scryfall name-guess.
 * `just token-art-gaps` reports the weaker-but-more-useful property — which tokens are showing
 * generic stand-in art rather than their own set's.
 */
class TokenArtCoverageTest : FunSpec({

    val tokenArtRegistry = TokenArtRegistry().apply {
        for (set in MtgSetCatalog.all) {
            register(set.code, TokenArtData.forSet(set), set.cards.map { it.name })
        }
    }

    /** Art the predefined-token executor falls back to, keyed by `tokenType`. */
    val predefinedArt: Map<String, String?> =
        PredefinedTokens.allTokens.associate { it.name to it.metadata.imageUri }

    test("every token a card can create resolves to an image") {
        val gaps = mutableListOf<String>()

        for (set in MtgSetCatalog.all) {
            for (card in set.cards) {
                for (site in TokenCreationSites.of(card)) {
                    // An explicit per-card override always wins; a resolution-time chosen creature
                    // type has no statically-known identity, so the executor falls back at runtime.
                    if (site.explicitImageUri != null || site.chosenType) continue

                    val setScoped = tokenArtRegistry.resolve(
                        sourceCardDefinitionId = card.name,
                        tokenName = site.tokenName,
                        power = site.power,
                        toughness = site.toughness,
                        colors = site.colors,
                    )
                    val resolved = setScoped
                        ?: if (site.predefined) predefinedArt[site.tokenName]
                        else TokenArt.forCreatureTypes(site.creatureTypes)

                    if (resolved == null) {
                        gaps += "[${set.code}] ${card.name} -> ${site.tokenName.ifEmpty { "<no type>" }}"
                    }
                }
            }
        }

        if (gaps.isNotEmpty()) {
            val kinds = gaps.map { it.substringAfterLast("-> ") }.distinct().sorted()
            println("=== tokens with no art: ${gaps.size} across ${kinds.size} kinds ===")
            gaps.distinct().sorted().forEach { println("  $it") }
            println("--- add these to TokenArt.IMAGES (or the set's tokenArt) ---")
            kinds.forEach { println("  $it") }
        }
        gaps.shouldBeEmpty()
    }
})
