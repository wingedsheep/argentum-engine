package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Willbreaker
 * {3}{U}{U}
 * Creature — Human Wizard
 * 2/3
 * Whenever a creature an opponent controls becomes the target of a spell or ability you control,
 * gain control of that creature for as long as you control this creature.
 *
 * The trigger is the filter-scoped becomes-target event narrowed to targeting objects you control
 * (`byYou`), so it fires per opponent's creature targeted — by a spell or by any ability — and a
 * multi-target spell steals each matching creature. The steal reads the targeted creature back as
 * the triggering entity and lasts [Duration.WhileYouControlSource]: it never takes hold if you no
 * longer control Willbreaker when the trigger resolves, and it ends for good once you stop
 * controlling it (the 2015 rulings).
 */
val Willbreaker = card("Willbreaker") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Whenever a creature an opponent controls becomes the target of a spell or ability you control, " +
        "gain control of that creature for as long as you control this creature."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).becomesTarget(byYou = true)
        effect = Effects.GainControl(EffectTarget.TriggeringEntity, Duration.WhileYouControlSource("this creature"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Dan Murayama Scott"
        flavorText = "\"Master your mind, or I shall master it for you.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2465eafc-453c-4e4f-b369-0951b8a4b7bf.jpg?1783938345"
        ruling("2015-06-22", "If Willbreaker leaves the battlefield, you no longer control it, and its control-change effect ends.")
        ruling("2015-06-22", "If you lose control of Willbreaker before its ability resolves, you won't gain control of the creature at all.")
        ruling("2015-06-22", "If another player gains control of Willbreaker, its control-change effect ends. Regaining control of Willbreaker won't cause you to regain control of the creature.")
    }
}
