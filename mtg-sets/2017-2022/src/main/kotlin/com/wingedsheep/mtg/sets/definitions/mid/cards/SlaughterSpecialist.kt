package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slaughter Specialist (Innistrad: Midnight Hunt #122)
 * {1}{B}
 * Creature — Vampire Warrior
 * 3/3
 * When this creature enters, each opponent creates a 1/1 white Human creature token.
 * Whenever a creature an opponent controls dies, put a +1/+1 counter on this creature.
 *
 * The token's `controller` is [Player.EachOpponent]; the create-token executor fans it out so
 * every opponent creates (and controls) one Human.
 */
val SlaughterSpecialist = card("Slaughter Specialist") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Warrior"
    oracleText = "When this creature enters, each opponent creates a 1/1 white Human creature token.\n" +
        "Whenever a creature an opponent controls dies, put a +1/+1 counter on this creature."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human"),
            controller = EffectTarget.PlayerRef(Player.EachOpponent),
            imageUri = "https://cards.scryfall.io/normal/front/b/7/b7667345-e11b-4cad-ac4c-84eb1c5656c5.jpg?1783925225",
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Kari Christensen"
        flavorText = "From every kill, a gruesome trophy."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb83e33c-810e-4de3-800d-2038f38a1eda.jpg?1783925610"
    }
}
