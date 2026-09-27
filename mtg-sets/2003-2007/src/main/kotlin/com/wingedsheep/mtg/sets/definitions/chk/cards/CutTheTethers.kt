package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cut the Tethers
 * {2}{U}{U}
 * Sorcery
 * For each Spirit, return it to its owner's hand unless that player pays {3}.
 *
 * "That player" is the Spirit's **owner**, not its controller — the two differ for a stolen Spirit.
 * There is no owner-of-the-iterated-entity player reference, so the loop is turned inside out:
 * `ForEachPlayer(ActivePlayerFirst)` rebinds "you" to each player in APNAP order, and the inner
 * `ForEachInGroup` walks the Spirits *that player owns* (`ownedByYou()`), charging the toll to the
 * rebound controller. Every Spirit is visited exactly once, by its owner.
 *
 * A bare "Spirit" names a Spirit *permanent* (any card type), hence `Permanent.withSubtype`.
 * Paying is a mana cost, so a player who can't produce {3} is never asked and the Spirit returns.
 */
val CutTheTethers = card("Cut the Tethers") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "For each Spirit, return it to its owner's hand unless that player pays {3}."

    spell {
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            listOf(
                Effects.ForEachInGroup(
                    filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Spirit").ownedByYou()),
                    effect = Effects.PayOrSuffer(
                        cost = Costs.pay.Mana("{3}"),
                        suffer = Effects.ReturnToHand(EffectTarget.IterationEntity),
                        player = EffectTarget.Controller,
                        consequenceDescription = "have this Spirit returned to your hand",
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Ron Spears"
        flavorText = "\"You cannot bar the path of gods. You can only divert their journey for a while.\"\n—Sensei Hisoka"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4ff18307-9ef8-4e3a-89e9-cb8c04997222.jpg?1783944328"
    }
}
