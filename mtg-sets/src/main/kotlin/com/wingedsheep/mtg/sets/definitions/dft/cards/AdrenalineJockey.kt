package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Adrenaline Jockey — Aetherdrift #112
 * {2}{R} · Creature — Minotaur Pilot · 3/3
 */
val AdrenalineJockey = card("Adrenaline Jockey") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Pilot"
    power = 3
    toughness = 3
    oracleText = "Whenever a player casts a spell, if it's not their turn, this creature deals 4 " +
        "damage to them.\nWhenever you activate an exhaust ability, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.AnyPlayerCastsSpell
        triggerCondition = Conditions.Not(Conditions.IsPlayersTurn(Player.TriggeringPlayer))
        effect = Effects.DealDamage(4, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    triggeredAbility {
        trigger = Triggers.YouActivateExhaustAbility
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Alfonso Santano"
        flavorText = "\"Either I win or we both lose. Take your pick.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8655373-320d-440d-b700-d03413f743fd.jpg?1783907888"

        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns, it becomes a new object, so its exhaust ability can be activated again."
        )
        ruling(
            "2025-02-07",
            "An ability that triggers when you activate an exhaust ability resolves before that exhaust ability."
        )
    }
}
