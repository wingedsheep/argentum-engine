package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crumble to Dust — Battle for Zendikar #128
 * {3}{R} · Sorcery
 *
 * Devoid (This card has no color.)
 * Exile target nonbasic land. Search its controller's graveyard, hand, and library for any number
 * of cards with the same name as that land and exile them. Then that player shuffles.
 *
 * Maelstrom Pulse's capture-the-target's-name step feeding Lost Legacy's "any number" search. The
 * name is recorded while the land is still on the battlefield; the land is exiled before the
 * search, as printed. "Its controller" is the land's last-known controller, read after it has left
 * the battlefield — for a stolen land, the thief's zones are searched, not the owner's.
 */
val CrumbleToDust = card("Crumble to Dust") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Exile target nonbasic land. Search its controller's graveyard, hand, and library for any number " +
        "of cards with the same name as that land and exile them. Then that player shuffles."

    keywords(Keyword.DEVOID)

    spell {
        val land = target(TargetFilter.NonbasicLand)
        val itsController = Player.ControllerOf("target nonbasic land")
        effect = Effects.Pipeline {
            val targeted = gather(CardSource.ChosenTargets)
            val landName = storeCardName(targeted)
            run(Effects.Exile(land))
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = itsController,
                    filter = GameObjectFilter.Any.namedFromVariable(landName)
                ),
                search = true
            )
            val toExile = chooseAnyNumber(
                from = matches,
                prompt = "Choose any number of cards with that name to exile",
                selectedLabel = "Exile"
            )
            exile(toExile, owner = itsController)
            run(Effects.ShuffleLibrary(target = EffectTarget.PlayerRef(itsController)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63352c11-807d-4878-a975-02ef451c3184.jpg?1783938198"
        ruling("2015-08-25", "You may leave any cards with that name in the player's graveyard, hand, or library. You don't have to exile them.")
    }
}
