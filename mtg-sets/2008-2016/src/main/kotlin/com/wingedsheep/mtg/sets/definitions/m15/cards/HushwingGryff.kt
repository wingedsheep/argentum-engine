package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressZoneChangeTriggers

/**
 * Hushwing Gryff — Magic 2015 #15
 * {2}{W} · Creature — Hippogriff · 2/1 · Rare
 *
 * Flash
 * Flying
 * Creatures entering don't cause abilities to trigger.
 *
 * Torpor Orb on a flash flier: the same [SuppressZoneChangeTriggers] static at its default
 * `filter = Creature`. The printed rulings are the shared static's documented behaviour —
 * replacement effects and "as this enters" choices are untouched, the entering permanent is read
 * in projected state, and simultaneous entries (the Gryff alongside another creature) are both
 * suppressed.
 */
val HushwingGryff = card("Hushwing Gryff") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Hippogriff"
    power = 2
    toughness = 1
    oracleText = "Flash\nFlying\nCreatures entering don't cause abilities to trigger."

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = SuppressZoneChangeTriggers()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "John Severin Brassell"
        flavorText = "An overwhelming sense of calm accompanies the gryffs that wheel above the roofs of Gavony."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b44eb0d-5a3a-4624-aee4-11d6978fb4b0.jpg?1783939202"

        ruling(
            "2014-07-18",
            "Abilities that create replacement effects, such as a permanent entering the battlefield " +
                "tapped or with counters on it, are unaffected."
        )
        ruling(
            "2014-07-18",
            "Abilities that apply \"as [this creature] enters the battlefield,\" such as choosing a " +
                "creature to copy with Mercurial Pretender, are also unaffected."
        )
        ruling(
            "2014-07-18",
            "Hushwing Gryff's ability stops a creature's own enters-the-battlefield triggered abilities " +
                "as well as other triggered abilities that would trigger when a creature enters the battlefield."
        )
        ruling(
            "2014-07-18",
            "The trigger event doesn't have to specify \"creatures\" entering the battlefield. For " +
                "example, Amulet of Vigor says \"Whenever a permanent enters the battlefield tapped and " +
                "under your control, untap it.\" If a creature enters the battlefield tapped and under " +
                "your control, Amulet of Vigor's ability would not trigger. If a land (that isn't also " +
                "a creature) enters the battlefield tapped and under your control, Amulet of Vigor's " +
                "ability would trigger."
        )
        ruling(
            "2014-07-18",
            "Look at the permanent as it exists on the battlefield, taking into account continuous " +
                "effects, to determine whether any triggered abilities will trigger. For example, if " +
                "you control March of the Machines, which says, in part, \"Each noncreature artifact " +
                "is an artifact creature,\" each artifact will be a creature at the time it enters the " +
                "battlefield and will not cause triggered abilities to trigger."
        )
        ruling(
            "2014-07-18",
            "If Hushwing Gryff and another creature enter the battlefield at the same time, neither " +
                "creature entering the battlefield will cause triggered abilities to trigger."
        )
    }
}
