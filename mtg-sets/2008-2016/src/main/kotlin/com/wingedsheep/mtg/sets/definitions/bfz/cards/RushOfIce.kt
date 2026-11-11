package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rush of Ice
 * {U}
 * Sorcery
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Awaken 3—{4}{U} (If you cast this spell for {4}{U}, also put three +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The "doesn't untap" duration is keyed to whoever controls the creature when an untap step comes
 * around, so a creature that changes controllers stays locked through its new controller's next
 * untap step, as the rulings say. Casting for the awaken cost adds the land target after the
 * creature target and runs the awaken ability after the tap.
 */
val RushOfIce = card("Rush of Ice") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap target creature. It doesn't untap during its controller's next untap step.\n" +
        "Awaken 3—{4}{U} (If you cast this spell for {4}{U}, also put three +1/+1 counters on target " +
        "land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(3, "{4}{U}"))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, creature, Duration.UntilAfterAffectedControllersNextUntap())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9ae6f63-d1b2-4bf9-a423-ed6fbb540d4b.jpg?1783938207"
        ruling("2015-08-25", "Rush of Ice can target a creature that's already tapped. It still won't untap during its controller's next untap step.")
        ruling("2015-08-25", "Rush of Ice tracks the creature, but not its controller. If the creature changes controllers before its first controller's next untap step has come around, then it won't untap during its new controller's next untap step.")
    }
}
