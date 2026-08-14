package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rage into the Valley — The Hobbit #79
 * {2}{B} · Sorcery · Common
 *
 * You draw a card and lose 1 life.
 * Amass Goblins 2.
 *
 * "You draw a card and lose 1 life" is one atomic clause aimed at the caster, so both halves target
 * the controller explicitly rather than leaning on the "target opponent" default of
 * [Effects.LoseLife]. `Effects.Amass` handles the Army bookkeeping (create a 0/0 black Goblin Army
 * first if you control none, otherwise add the counters to the Army you already have).
 */
val RageIntoTheValley = card("Rage into the Valley") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw a card and lose 1 life.\n" +
        "Amass Goblins 2. (Put two +1/+1 counters on an Army you control. It's also a Goblin. " +
        "If you don't control an Army, create a 0/0 black Goblin Army creature token first.)"

    spell {
        effect = Effects.DrawCards(1) then
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You)) then
            Effects.Amass(2, "Goblin")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Antonio José Manzanedo"
        flavorText = "The Goblin army poured in, driving wildly between the arms of the Mountain, " +
            "seeking for the foe."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8651958c-3b94-47a9-a751-faf8f6236a42.jpg?1785496290"
    }
}
