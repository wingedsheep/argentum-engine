package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Grip of the Roil
 * {2}{U}
 * Instant
 * Surge {1}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Draw a card.
 *
 * The "doesn't untap" duration is keyed to whoever controls the creature when an untap step comes
 * around, so a creature that changes controllers stays locked through its new controller's next
 * untap step, as the 2016-01-22 ruling says.
 */
val GripOfTheRoil = card("Grip of the Roil") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Surge {1}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Tap target creature. It doesn't untap during its controller's next untap step.\n" +
        "Draw a card."

    keywordAbility(KeywordAbility.surge("{1}{U}"))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, creature, Duration.UntilAfterAffectedControllersNextUntap()) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cf33069-013f-43ab-9a31-e67bb6dc996d.jpg?1783937918"
        ruling("2016-01-22", "Grip of the Roil can target a creature that's already tapped. It still won't untap during its controller's next untap step.")
        ruling("2016-01-22", "Grip of the Roil tracks the creature, but not its controller. If the creature changes controllers before its first controller's next untap step has come around, then it won't untap during its new controller's next untap step.")
    }
}
