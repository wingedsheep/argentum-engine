package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flameskull — Adventures in the Forgotten Realms #143
 * {1}{R}{R} · Creature — Skeleton 3/1
 *
 * Flying
 * This creature can't block.
 * Rejuvenation — When this creature dies, exile it. If you do, exile the top card of your library.
 * Until the end of your next turn, you may play one of those cards.
 *
 * "Exile it" only reaches Flameskull while it is still in a graveyard (ruling: if it left the
 * graveyard before the ability resolves, nothing is exiled) — the gather intersects the source
 * with every graveyard. "One of those cards" is a single `maxPlays = 1` grant over *both* exiled
 * cards, so playing either one revokes the other. The pipeline has no union step, so the pair is
 * built as a set intersection over everything in exile: exile ∖ ((exile ∖ Flameskull) ∖ top card).
 */
val Flameskull = card("Flameskull") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Skeleton"
    power = 3
    toughness = 1
    oracleText = "Flying\nThis creature can't block.\n" +
        "Rejuvenation — When this creature dies, exile it. If you do, exile the top card of your " +
        "library. Until the end of your next turn, you may play one of those cards. (If you cast " +
        "Flameskull this way, you can't play the other card, and vice versa.)"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val inGraveyards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each))
                val self = gather(CardSource.Entity(EffectTarget.Self))
                // Named: a stolen Flameskull goes to its owner's exile, so "if you do" reads what
                // actually moved rather than the growth of your own exile.
                moveTracked(
                    exclude(inGraveyards, exclude(inGraveyards, self)),
                    CardDestination.ToZone(Zone.EXILE),
                    name = "flameskullExiled"
                )
            },
            successCriterion = SuccessCriterion.CollectionNonEmpty("flameskullExiled"),
            then = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(1))
                val exiledTop = moveTracked(top, CardDestination.ToZone(Zone.EXILE))
                val self = gather(CardSource.Entity(EffectTarget.Self))
                val inExile = gather(CardSource.FromZone(Zone.EXILE, Player.Each))
                val thoseCards = exclude(inExile, exclude(exclude(inExile, self), exiledTop))
                run(
                    Effects.GrantMayPlayFromExile(
                        from = thoseCards,
                        expiry = MayPlayExpiry.UntilEndOfNextTurn,
                        maxPlays = 1
                    )
                )
            },
            descriptionOverride = "Rejuvenation — When this creature dies, exile it. If you do, exile " +
                "the top card of your library. Until the end of your next turn, you may play one of " +
                "those cards."
        )
        description = "Rejuvenation — When this creature dies, exile it. If you do, exile the top card " +
            "of your library. Until the end of your next turn, you may play one of those cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "143"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32d3f766-4098-4023-9a23-e9ff6722b66d.jpg?1783926479"
        ruling("2021-07-23", "Exiling Flameskull as its triggered ability resolves isn't optional, but if Flameskull leaves the graveyard before the ability starts resolving, you won't exile it or the top card of your library.")
        ruling("2021-07-23", "You must still pay all costs and follow all timing rules for spells cast this way. Likewise, playing lands this way is subject to the normal rules for playing lands.")
    }
}
