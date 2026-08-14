package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Concealed Weapon — Murders at Karlov Manor #117
 * {1}{R} · Artifact — Equipment
 *
 * Equipped creature gets +3/+0.
 * Disguise {2}{R}
 * When this Equipment is turned face up, attach it to target creature you control.
 * Equip {1}{R}
 *
 * The set's one **noncreature** disguise card, and the reason it works is that face-down-ness is a
 * characteristic-defining effect in the projection, not a property of the card: CR 708.2 gives *any*
 * face-down permanent a 2/2 creature body with no name or subtypes, so the Equipment spends its time
 * face down as a 2/2 with ward {2} and reverts to being a (creature-less) Equipment on the flip.
 * Neither the face-down cast path nor the turn-up procedure gates on the card being a creature —
 * only manifest/cloak's "pay the mana cost" route does, which disguise doesn't use.
 *
 * The flip is a two-for-one: {3} gets a surprise 2/2 blocker down, then {2}{R} turns it into a real
 * Equipment *and* attaches it for free. That attachment is a genuine triggered ability
 * ([Triggers.TurnedFaceUp]) rather than a `disguiseFaceUpEffect` replacement, because the oracle text
 * says "When", and it matters here: it uses the stack, it targets, and per the official ruling it is
 * *not* an equip activation — no mana, and none of equip's sorcery-speed timing restriction. So the
 * Equipment can arrive attached mid-combat.
 *
 * Two edge cases the shape gets right for free. Flipping is legal with **no** creatures at all: the
 * special action isn't gated on the trigger having a target, the targetless trigger is simply removed
 * from the stack, and the Equipment stays on the battlefield unattached. Likewise if the chosen
 * creature leaves before the trigger resolves — the trigger fizzles and the Equipment sits unattached
 * until someone pays equip.
 */
val ConcealedWeapon = card("Concealed Weapon") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+0.\n" +
        "Disguise {2}{R} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)\n" +
        "When this Equipment is turned face up, attach it to target creature you control.\n" +
        "Equip {1}{R}"

    staticAbility {
        ability = ModifyStats(+3, +0, Filters.EquippedCreature)
    }

    disguise = "{2}{R}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = Targets.CreatureYouControl
        effect = Effects.AttachEquipment()
        description = "When this Equipment is turned face up, attach it to target creature you control."
    }

    equipAbility("{1}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Nicholas Elias"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38e31fa6-a445-47c6-a73f-135087f6d760.jpg?1783912886"

        ruling(
            "2024-02-02",
            "Attaching Concealed Weapon with its triggered ability isn't the same as using its " +
                "equip ability. You don't pay mana for the attachment, and the timing restrictions " +
                "for equip abilities don't apply."
        )
        ruling(
            "2024-02-02",
            "If the target creature becomes an illegal target, Concealed Weapon remains on the " +
                "battlefield unattached."
        )
        ruling(
            "2024-02-02",
            "You may turn Concealed Weapon face up even if you don't control any creatures."
        )
        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
    }
}
