package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Workshop Elders — Throne of Eldraine #318
 * {6}{U} · Creature — Human Artificer · 4/4
 *
 * Artifact creatures you control have flying.
 * At the beginning of combat on your turn, you may have target noncreature artifact you control
 * become a 0/0 artifact creature. If you do, put four +1/+1 counters on it.
 *
 * The optional trigger targets on the stack and asks yes/no at resolution; declining skips both
 * the animation and the counters ("If you do"). The animation has no printed duration, so it is
 * permanent — `BecomeCreature` adds CREATURE while keeping the artifact type, base P/T 0/0 in
 * layer 7b, and the counters make it 4/4. Same shape as Tawnos's Tinkering.
 */
val WorkshopElders = card("Workshop Elders") {
    manaCost = "{6}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    power = 4
    toughness = 4
    oracleText = "Artifact creatures you control have flying.\n" +
        "At the beginning of combat on your turn, you may have target noncreature artifact you " +
        "control become a 0/0 artifact creature. If you do, put four +1/+1 counters on it."

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        optional = true
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.notCreature().youControl()))
        effect = Effects.BecomeCreature(
            target = artifact,
            power = 0,
            toughness = 0,
            duration = Duration.Permanent,
        ) then Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 4, artifact)
        description = "At the beginning of combat on your turn, you may have target noncreature " +
            "artifact you control become a 0/0 artifact creature. If you do, put four +1/+1 " +
            "counters on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "318"
        artist = "Iain McCaig"
        flavorText = "\"Fear not what's under the bed. The toys are watching.\"\n—*Beyond the Great Henge*"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53913e26-d98b-4ca6-aeea-23633d762d46.jpg?1783932551"
        ruling(
            "2019-10-04",
            "The effect of Workshop Elders's triggered ability lasts indefinitely. It doesn't wear off during the cleanup step.",
        )
        ruling(
            "2019-10-04",
            "An artifact that becomes a creature due to Workshop Elders can't attack unless you've controlled it " +
                "continuously since your turn began.",
        )
        ruling(
            "2019-10-04",
            "If another effect later causes the artifact to become a creature and sets its power and toughness as it " +
                "does so, that creature will have that base power and toughness; it won't be 0/0. Notably, crewing a " +
                "Vehicle does not set its power and toughness, so a Vehicle's base power and toughness remain 0/0 if " +
                "you crew it.",
        )
    }
}
