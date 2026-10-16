package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val GlowingOne = card("Glowing One") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Zombie Mutant"
    power = 2
    toughness = 2
    oracleText = "Deathtouch\n" +
        "Whenever this creature deals combat damage to a player, they get four rad counters.\n" +
        "Whenever a player mills a nonland card, you gain 1 life."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.RAD, 4, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    // Per card, not per mill instruction: milling four nonland cards triggers it four times.
    triggeredAbility {
        trigger = Triggers.anyPlayer.mills(GameObjectFilter.Nonland)
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Liiga Smilshkalne"
        flavorText = "The glowing one mumbles something and then falls silent. He drools uncontrollably."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1e36b92-aa88-4536-aebc-7e84a10d73fe.jpg?1783912398"
        ruling(
            "2024-03-08",
            "If a player is instructed to mill more than one card, Glowing One's last ability will trigger once for each nonland card they mill during that instruction."
        )
    }
}
