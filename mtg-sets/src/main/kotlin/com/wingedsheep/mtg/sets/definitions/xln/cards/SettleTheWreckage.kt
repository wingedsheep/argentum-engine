package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Settle the Wreckage
 * {2}{W}{W}
 * Instant
 *
 * Exile all attacking creatures target player controls. That player may search their library for
 * that many basic land cards, put those cards onto the battlefield tapped, then shuffle.
 *
 * The spell targets **only the player** (CR ruling 2017-09-29), so hexproof/protected attackers are
 * still exiled — the attackers are gathered as a group at resolution rather than targeted. The
 * gather is scoped by [GameObjectFilter.targetPlayerControls] against the declared player target
 * so a stolen attacker follows its *controller*, matching the printed wording.
 *
 * "That many" counts the attacking creatures that were **exiled**, which per the third ruling
 * includes tokens and any creature that never actually reached exile — so the count reads off the
 * gathered collection, not off what landed in the exile zone. The fetch itself is a `ChooseUpTo`,
 * so the player may find fewer basics than that (ruling two).
 *
 * The search is spelled out inline rather than reusing `Patterns.Library.searchLibrary` under an
 * [Effects.ForEachPlayer] rebinding, because **`ForEach` over players deliberately wipes
 * `storedCollections`** for each iteration — the `attackers` count would read 0 inside the loop and
 * the fetch would silently find nothing. Everything therefore stays in one pipeline scope, and the
 * target player is named explicitly instead: [Player.TargetPlayer] scopes the library gather, the
 * battlefield destination, and the shuffle, while [Chooser.TargetPlayer] makes *them* pick the
 * cards. The `may` is a [MayEffect] whose `decisionMaker` is the target player (only the prompt is
 * delegated — the effect still resolves under the caster, which is why every step names its player).
 *
 * Deviation, deliberate: with zero attackers exiled the search is skipped instead of prompting.
 * "Search for zero basic lands, then shuffle" can find nothing, and a no-op library-search prompt
 * on every empty resolution is worse than losing the theoretical free shuffle.
 */
val SettleTheWreckage = card("Settle the Wreckage") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile all attacking creatures target player controls. That player may search " +
        "their library for that many basic land cards, put those cards onto the battlefield " +
        "tapped, then shuffle."

    spell {
        val player = target("target player", Targets.Player)
        effect = Effects.Pipeline {
            val attackers = gather(
                GameObjectFilter.Creature.attacking().targetPlayerControls(player),
                name = "attackers"
            )
            exile(attackers)
            ifNotEmpty(attackers) {
                run(
                    MayEffect(
                        Effects.Pipeline {
                            val library = gather(
                                CardSource.FromZone(
                                    Zone.LIBRARY,
                                    Player.TargetPlayer,
                                    GameObjectFilter.BasicLand
                                ),
                                name = "searchable"
                            )
                            val found = chooseUpTo(
                                DynamicAmounts.distinctEntitiesIn(attackers.key),
                                from = library,
                                chooser = Chooser.TargetPlayer,
                                prompt = "Search your library for basic land cards",
                                name = "found"
                            )
                            move(
                                found,
                                CardDestination.ToZone(
                                    Zone.BATTLEFIELD,
                                    Player.TargetPlayer,
                                    ZonePlacement.Tapped
                                )
                            )
                            run(ShuffleLibraryEffect(EffectTarget.PlayerRef(Player.TargetPlayer)))
                        },
                        decisionMaker = EffectTarget.PlayerRef(Player.TargetPlayer),
                        descriptionOverride = "That player may search their library for that many " +
                            "basic land cards, put those cards onto the battlefield tapped, then shuffle."
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Dimitar Marinski"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cbd346e-098a-4cf6-a72f-468376fd2e8f.jpg?1783935791"
        ruling(
            "2017-09-29",
            "Settle the Wreckage targets only the player. Creatures with hexproof that player " +
                "controls will be exiled as this spell resolves."
        )
        ruling(
            "2017-09-29",
            "That player can find fewer basic land cards than the number of exiled creatures, " +
                "whether because they want to or because they don't have that many basic land " +
                "cards left."
        )
        ruling(
            "2017-09-29",
            "The number of lands that player may find is the number of attacking creatures that " +
                "were exiled, even if some of those creatures were tokens, weren't creature " +
                "cards, or didn't end up in exile."
        )
    }
}
