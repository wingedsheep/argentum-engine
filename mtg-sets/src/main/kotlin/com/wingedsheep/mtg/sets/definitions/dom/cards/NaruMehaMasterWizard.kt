package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Naru Meha, Master Wizard
 * {2}{U}{U}
 * Legendary Creature — Human Wizard
 * 3/3
 * Flash
 * When Naru Meha, Master Wizard enters the battlefield, copy target instant or sorcery spell
 * you control. You may choose new targets for the copy.
 * Other Wizards you control get +1/+1.
 */
val NaruMehaMasterWizard = card("Naru Meha, Master Wizard") {
    manaCost = "{2}{U}{U}"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "Flash\nWhen Naru Meha, Master Wizard enters, copy target instant or sorcery spell you control. You may choose new targets for the copy.\nOther Wizards you control get +1/+1."

    keywords(Keyword.FLASH)

    // When Naru Meha enters, copy target instant or sorcery spell you control
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = Targets.InstantOrSorcerySpellYouControl
        effect = Effects.CopyTargetSpell()
    }

    // Other Wizards you control get +1/+1
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Wizard"), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "59"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57168712-73dc-411c-9d13-f0c43202cb9a.jpg?1562735983"
        ruling("2018-04-27", "Naru Meha's triggered ability can copy any instant or sorcery spell you control, not just one with targets.")
        ruling("2018-04-27", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2018-04-27", "The copy will have the same targets as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them.")
        ruling("2018-04-27", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode. A different mode can't be chosen.")
        ruling("2018-04-27", "If the spell that's copied has an X whose value was determined as it was cast, the copy will have the same value of X.")
    }
}
