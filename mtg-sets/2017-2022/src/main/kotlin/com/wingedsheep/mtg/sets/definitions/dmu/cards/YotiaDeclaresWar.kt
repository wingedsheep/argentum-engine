package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Yotia Declares War
 * {1}{R}
 * Enchantment — Saga
 *
 * Read ahead (Choose a chapter and start with that many lore counters. Add one after your draw
 * step. Skipped chapters don't trigger. Sacrifice after III.)
 * I — Create a 0/2 colorless Thopter artifact creature token with flying named Ornithopter.
 * II — Tap any number of untapped artifacts you control. When you do, this Saga deals that much
 *      damage to target creature or planeswalker.
 * III — Up to one target artifact you control becomes an artifact creature with base power and
 *       toughness 4/4 until end of turn.
 *
 * Chapter II is Invasion of Tarkir's shape: the action taps any number (zero allowed) of
 * untapped artifacts into a named collection, then the reflexive "when you do" trigger (CR 603.12)
 * goes on the stack with its target chosen then and deals the collection's size. Per the ruling,
 * tapping zero still triggers it (for 0 damage), so the trigger is not optional.
 * Chapter III carries Oracle errata: the printed card omitted "artifact" from "an artifact creature".
 */
private val tappedArtifacts = CollectionSlot("yotiaTappedArtifacts")

val YotiaDeclaresWar = card("Yotia Declares War") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I — Create a 0/2 colorless Thopter artifact creature token with flying named Ornithopter.\n" +
        "II — Tap any number of untapped artifacts you control. When you do, this Saga deals that " +
        "much damage to target creature or planeswalker.\n" +
        "III — Up to one target artifact you control becomes an artifact creature with base power " +
        "and toughness 4/4 until end of turn."

    readAhead()

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 2,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            name = "Ornithopter",
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/a/6/a63d3174-75af-4dfa-b2f9-c4330c755af1.jpg?1783921125",
        )
    }

    sagaChapter(2) {
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val untapped = gather(
                    CardSource.ControlledPermanents(Player.You, GameObjectFilter.Artifact.untapped())
                )
                val tapped = chooseAnyNumber(
                    from = untapped,
                    chooser = Chooser.Controller,
                    useTargetingUI = true,
                    prompt = "Tap any number of untapped artifacts you control",
                    name = "yotiaTappedArtifacts",
                )
                run(Effects.TapCollection(tapped, tap = true))
            },
            optional = false,
            descriptionOverride = "Tap any number of untapped artifacts you control. When you do, " +
                "this Saga deals that much damage to target creature or planeswalker.",
        ) {
            val victim = target(Targets.CreatureOrPlaneswalker)
            effect = Effects.DealDamage(tappedArtifacts.count, victim)
        }
    }

    sagaChapter(3) {
        val artifact = target(TargetFilter.Artifact.youControl(), optional = true)
        effect = Effects.BecomeCreature(
            target = artifact,
            power = 4,
            toughness = 4,
            addTypes = setOf("ARTIFACT"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bd9e99a-ae8c-4323-aa86-b19288c877d4.jpg?1783921306"

        ruling("2022-09-09", "You don't choose the target creature or planeswalker as you put the second chapter ability of Yotia Declares War on the stack. First, you tap any number of artifacts. Then, a second \"reflexive\" ability triggers (denoted by the phrase \"when you do\") that requires a target. Players may respond to the reflexive triggered ability as normal, and they will know how many artifacts were tapped when they do.")
        ruling("2022-09-09", "If you choose to tap zero artifacts as the second chapter ability resolves, the reflexive triggered ability still triggers, but deals no damage to the target creature or planeswalker.")
        ruling("2022-09-09", "Yotia Declares War was mistakenly printed with the word \"artifact\" omitted from its final chapter ability, and it has received an update to its Oracle text.")
        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
