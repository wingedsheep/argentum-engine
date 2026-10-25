package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Blackstaff of Waterdeep — Adventures in the Forgotten Realms #48
 * {U} · Legendary Artifact · Rare
 *
 * Animate Walking Statue keeps the target's existing types (it's already an artifact) and lasts only
 * while the Blackstaff remains tapped; MAY_NOT_UNTAP lets its controller keep it tapped.
 */
val TheBlackstaffOfWaterdeep = card("The Blackstaff of Waterdeep") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact"
    oracleText = "You may choose not to untap The Blackstaff of Waterdeep during your untap step.\n" +
        "Animate Walking Statue — {1}{U}, {T}: Another target nontoken artifact you control becomes a " +
        "4/4 artifact creature for as long as The Blackstaff of Waterdeep remains tapped. " +
        "Activate only as a sorcery."

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.youControl().nontoken()).other())
        effect = Effects.BecomeCreature(
            target = artifact,
            power = 4,
            toughness = 4,
            duration = Duration.WhileSourceTapped("The Blackstaff of Waterdeep"),
        )
        description = "Animate Walking Statue — Another target nontoken artifact you control becomes a " +
            "4/4 artifact creature for as long as The Blackstaff of Waterdeep remains tapped. " +
            "Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3317cbf3-13a1-4471-a23c-de429941e8a4.jpg?1783926518"
    }
}
