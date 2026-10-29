package com.wingedsheep.mtg.sets.definitions.mic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion

/**
 * Wilhelt, the Rotcleaver — Innistrad: Midnight Hunt Commander #2 (canonical printing)
 * {2}{U}{B} · Legendary Creature — Zombie Warrior · 3/3
 *
 * Whenever another Zombie you control dies, if it didn't have decayed, create a 2/2 black Zombie
 * creature token with decayed.
 * At the beginning of your end step, you may sacrifice a Zombie. If you do, draw a card.
 *
 * - The "didn't have decayed" clause rides on the dies trigger's filter
 *   (`withoutKeyword(DECAYED)`), which a dies trigger answers from the creature's last-known
 *   information as it left the battlefield — so a decayed token never feeds Wilhelt, while a Zombie
 *   that merely *lost* decayed before dying would. The intervening-if recheck at resolution reads
 *   the same frozen last-known state, so it can never change the answer.
 * - The end-step clause is the Wanderwine Prophets shape: [Effects.May] over [Effects.IfYouDo]
 *   gated on a permanent actually being sacrificed. Wilhelt is a Zombie, so it may sacrifice itself.
 */
val WilheltTheRotcleaver = card("Wilhelt, the Rotcleaver") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Zombie Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever another Zombie you control dies, if it didn't have decayed, create a 2/2 " +
        "black Zombie creature token with decayed. (It can't block. When it attacks, sacrifice it at " +
        "end of combat.)\n" +
        "At the beginning of your end step, you may sacrifice a Zombie. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.another(
            GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).youControl().withoutKeyword(Keyword.DECAYED)
        ).dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
        )
        description = "Whenever another Zombie you control dies, if it didn't have decayed, create a " +
            "2/2 black Zombie creature token with decayed."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.SacrificeOwn(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE)),
                then = Effects.DrawCards(1),
                successCriterion = SuccessCriterion.PermanentsSacrificed
            )
        )
        description = "At the beginning of your end step, you may sacrifice a Zombie. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2501a911-d072-436d-ae3b-a5164e3b30aa.jpg?1783925384"
        ruling("2021-09-24", "Decayed represents a static ability and a triggered ability. “Decayed” means “This creature can't block” and “When this creature attacks, sacrifice it at end of combat.”")
        ruling("2021-09-24", "Once a creature with decayed attacks, it will be sacrificed at end of combat, even if it no longer has decayed at that time.")
    }
}
