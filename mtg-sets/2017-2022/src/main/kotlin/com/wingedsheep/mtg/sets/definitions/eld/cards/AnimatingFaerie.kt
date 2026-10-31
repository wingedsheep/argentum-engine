package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Animating Faerie // Bring to Life
 * {2}{U}
 * Creature — Faerie
 * 2/2
 *
 * Flying
 *
 * Adventure: Bring to Life — {2}{U}, Sorcery — Adventure
 * Target noncreature artifact you control becomes a 0/0 artifact creature. Put four +1/+1
 * counters on it.
 *
 * The animation is the same permanent `BecomeCreature` shape as Tawnos's Tinkering: no duration
 * is printed, so it lasts indefinitely (ruling). It adds the creature type while keeping the
 * artifact's own types and subtypes, and sets its base P/T to 0/0; the counters then make it 4/4.
 */
val AnimatingFaerie = card("Animating Faerie") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie"
    power = 2
    toughness = 2
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    adventure("Bring to Life") {
        manaCost = "{2}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target noncreature artifact you control becomes a 0/0 artifact creature. " +
            "Put four +1/+1 counters on it. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val artifact = target(TargetFilter(GameObjectFilter.Artifact.notCreature().youControl()))
            effect = Effects.BecomeCreature(
                target = artifact,
                power = 0,
                toughness = 0,
                duration = Duration.Permanent,
            ) then Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 4, artifact)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Joseph Meehan"
        flavorText = "A true puppet master has no need for strings."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32158458-42eb-41bc-a15a-11af28463eb0.jpg?1783932662"
        ruling("2019-10-04", "Bring to Life's effect lasts indefinitely. It doesn't wear off during the cleanup step.")
        ruling(
            "2019-10-04",
            "An artifact that becomes a creature due to Bring to Life can't attack unless you've controlled it " +
                "continuously since your turn began.",
        )
        ruling(
            "2019-10-04",
            "An Equipment that's also a creature can't be attached to anything. You can activate its equip ability, " +
                "but it won't become attached. If it's attached as it becomes a creature, it becomes unattached.",
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
