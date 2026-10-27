package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Command Beacon — Commander 2015 #56 (canonical printing)
 * Land
 *
 * {T}: Add {C}.
 * {T}, Sacrifice this land: Put your commander into your hand from the command zone.
 *
 * "Your commander" is a commander you own (CR 903.3c), gathered from your own command zone with
 * `.commander().ownedByYou()` so emblems and other command-zone objects are never offered. With
 * partners the player picks one of them (ruling); with no commander in the command zone (or no
 * commanders at all) the ability resolves and does nothing. Leaving the command zone this way isn't
 * a cast, so commander tax is untouched.
 */
val CommandBeacon = card("Command Beacon") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{T}, Sacrifice this land: Put your commander into your hand from the command zone."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            val commanders = gather(
                CardSource.FromZone(
                    zone = Zone.COMMAND,
                    player = Player.You,
                    filter = GameObjectFilter.Any.commander().ownedByYou(),
                ),
            )
            val chosen = chooseExactly(
                count = 1,
                from = commanders,
                prompt = "Choose your commander to put into your hand",
            )
            move(from = chosen, destination = CardDestination.ToZone(Zone.HAND))
        }
        description = "{T}, Sacrifice this land: Put your commander into your hand from the command zone."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Jonas De Ro"
        flavorText = "\"I gazed once into that light. Now it burns in my mind, forever denying me rest.\"\n" +
            "—Riku of Two Reflections"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/166fa02f-e456-48ca-8ac3-d217afceff4d.jpg?1783938103"

        ruling(
            "2020-11-10",
            "If you cast a commander from your hand, the commander tax doesn't apply. Additionally, that " +
                "casting won't add to commander tax if you later cast the commander from the command zone."
        )
        ruling(
            "2020-11-10",
            "If your commander isn't in the command zone (or you don't have a commander) as the last ability " +
                "resolves, nothing happens."
        )
        ruling(
            "2020-11-10",
            "If you have two commanders with the partner ability in the command zone, Command Beacon's effect " +
                "puts one of your choice into your hand, not both."
        )
    }
}
