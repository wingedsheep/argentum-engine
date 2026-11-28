package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Graveyard Shovel
 * {2}
 * Artifact
 * {2}, {T}: Target player exiles a card from their graveyard. If it's a creature card, you gain 2 life.
 *
 * Scrabbling Claws' tap ability (Gather → Select by [Chooser.TargetPlayer] → exile) followed by
 * Aether Rift's check on the moved card: [whenMatches] on the chosen collection gates the life gain,
 * which goes to the Shovel's controller, not the target player. An empty graveyard exiles nothing,
 * so no life is gained.
 */
val GraveyardShovel = card("Graveyard Shovel") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Target player exiles a card from their graveyard. If it's a creature card, " +
        "you gain 2 life."

    activatedAbility {
        val player = target(Targets.Player)
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
            val chosen = chooseExactly(
                1,
                from = graveyard,
                chooser = Chooser.TargetPlayer,
                prompt = "Exile a card from your graveyard"
            )
            exile(chosen, owner = player.asPlayer)
            run(Effects.If(
                condition = whenMatches(chosen, GameObjectFilter.Creature),
                then = Effects.GainLife(2)
            ))
        }
        description = "{2}, {T}: Target player exiles a card from their graveyard. If it's a creature " +
            "card, you gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Martina Pilcerova"
        flavorText = "Thanks to Havengul's thriving illegal market for corpses, the shovel that interred a " +
            "body during the day is usually the same one digging it out at night."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a4b8888-a10c-48b1-ba19-c041e5667b29.jpg?1783940901"
    }
}
