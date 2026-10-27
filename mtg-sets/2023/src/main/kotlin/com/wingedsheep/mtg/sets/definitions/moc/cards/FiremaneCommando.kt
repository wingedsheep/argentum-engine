package com.wingedsheep.mtg.sets.definitions.moc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val FiremaneCommando = card("Firemane Commando") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Angel Soldier"
    power = 4
    toughness = 3
    oracleText = "Flying\nWhenever you attack with two or more creatures, draw a card.\nWhenever another player attacks with two or more creatures, they draw a card if none of those creatures attacked you."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.attacks(minAttackers = 2)
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.player(Player.EachOtherPlayer).attacks(minAttackers = 2)
        effect = Effects.If(
            Conditions.Not(Conditions.TriggeringAttackersAttackedPlayer(Player.You)),
            Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/426a50de-13bb-4481-8643-ad492d3fc84a.jpg?1783917250"
        ruling("2023-04-14", "The last triggered ability cares only that none of the attacking creatures attacked you. The other player will draw a card if they attacked a planeswalker you control or a battle you protect with two or more creatures, for example.")
        ruling("2023-04-14", "No matter how many creatures another player attacks with, Firemane Commando's last ability will look at all of them to make sure none of them attacked you. If any of them did, the player won't draw a card.")
        ruling("2023-04-14", "For both triggered abilities, once the ability triggers, it doesn't matter what happens to the attacking creatures in response.")
    }
}
