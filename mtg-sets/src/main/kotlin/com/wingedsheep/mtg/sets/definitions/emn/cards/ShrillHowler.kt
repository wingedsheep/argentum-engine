package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shrill Howler // Howling Chorus (Eldritch Moon #168)
 * {2}{G}
 * Creature — Werewolf Horror 3/1 // Creature — Eldrazi Werewolf 3/5
 *
 * Front — "Creatures with power less than this creature's power can't block it."
 *         "{5}{G}: Transform this creature."
 * Back  — "Creatures with power less than this creature's power can't block it."
 *         "Whenever this creature deals combat damage to a player, create a 3/2 colorless Eldrazi
 *          Horror creature token."
 *
 * The evasion clause is the attacker-side static [CantBeBlockedByCreaturesWithLessPower] (Formation
 * Breaker / Elusive Otter). It compares *projected* power on both sides at the moment blockers are
 * declared, which is exactly the printed ruling — a later power change doesn't retroactively unblock
 * the attacker, because the restriction is only consulted during the declare-blockers legality check.
 * The clause is printed on **both** faces, so both faces carry it; the flip to a 3/5 lowers the
 * threshold from 3 to… still 3, but a pump on either face moves it.
 *
 * The back is a colorless Eldrazi Werewolf (no mana cost, no color indicator on the printed card);
 * `colorIdentity` stays "G" because identity is a property of the whole card. Its token is the
 * ordinary 3/2 colorless Eldrazi Horror, whose art comes from Eldritch Moon's synced token set.
 *
 * This is not a Daybound/Nightbound werewolf — the flip is an ordinary activated [TransformEffect].
 */

private val ShrillHowlerFront = card("Shrill Howler") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Werewolf Horror"
    power = 3
    toughness = 1
    oracleText = "Creatures with power less than this creature's power can't block it.\n" +
        "{5}{G}: Transform this creature."

    staticAbility {
        ability = CantBeBlockedByCreaturesWithLessPower()
    }

    activatedAbility {
        cost = Costs.Mana("{5}{G}")
        effect = TransformEffect(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Matt Stewart"
        flavorText = "\"A werewolf's howl is terrifying, to be sure. But this . . . this was a " +
            "chilling sound I somehow felt. I fear what will reply to it.\"\n" +
            "—Alena, trapper of Kessig"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a63c30c0-369a-4a75-b352-edab4d263d1b.jpg?1783937445"
        ruling(
            "2016-07-13",
            "The power of the blocking creature and that of Shrill Howler are compared only at the " +
                "moment that blockers are chosen. Changing either creature's power later won't cause " +
                "Shrill Howler to become unblocked."
        )
        ruling(
            "2016-07-13",
            "The power of the blocking creature and that of Howling Chorus are compared only at the " +
                "moment that blockers are chosen. Changing either creature's power later won't cause " +
                "Howling Chorus to become unblocked."
        )
    }
}

private val HowlingChorus = card("Howling Chorus") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Werewolf"
    power = 3
    toughness = 5
    oracleText = "Creatures with power less than this creature's power can't block it.\n" +
        "Whenever this creature deals combat damage to a player, create a 3/2 colorless Eldrazi " +
        "Horror creature token."

    staticAbility {
        ability = CantBeBlockedByCreaturesWithLessPower()
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Horror"),
        )
        description = "Whenever this creature deals combat damage to a player, create a 3/2 " +
            "colorless Eldrazi Horror creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/back/a/6/a63c30c0-369a-4a75-b352-edab4d263d1b.jpg?1783937445"
    }
}

val ShrillHowler: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ShrillHowlerFront,
    backFace = HowlingChorus,
)
