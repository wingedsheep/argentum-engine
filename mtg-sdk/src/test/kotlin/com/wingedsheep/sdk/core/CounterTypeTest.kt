package com.wingedsheep.sdk.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * [CounterType] is the one spelling of a counter kind: a canonical id that serializes, a printed
 * spelling that cards and the client read, and [CounterType.of] as the only way back from text.
 */
class CounterTypeTest : DescribeSpec({

    describe("printed") {
        it("spells every known kind the way the retired Counters constants did") {
            LEGACY_SPELLINGS.keys shouldBe CounterType.KNOWN.toSet() - NEW_KINDS
            LEGACY_SPELLINGS.filter { (kind, spelling) -> kind.printed != spelling }.keys.shouldBeEmpty()
        }

        it("spells the kinds added since in the same shape") {
            CounterType.BLOODLINE.printed shouldBe "bloodline"
            CounterType.INVITATION.printed shouldBe "invitation"
            CounterType.IMPOSTOR.printed shouldBe "impostor"
            CounterType.DEVOTION.printed shouldBe "devotion"
            CounterType.THEFT.printed shouldBe "theft"
            CounterType.TRAINING.printed shouldBe "training"
        }
    }

    describe("of") {
        it("round-trips every known kind from its id and from its printed spelling, in any case") {
            CounterType.KNOWN.filterNot { kind ->
                CounterType.of(kind.name) == kind &&
                    CounterType.of(kind.printed) == kind &&
                    CounterType.of(kind.printed.uppercase()) == kind &&
                    CounterType.of(kind.name.lowercase()) == kind
            }.shouldBeEmpty()
        }

        it("never falls back to +1/+1 for a name it does not know") {
            CounterType.of("+1+1") shouldNotBe CounterType.PLUS_ONE_PLUS_ONE
            (CounterType.of("+1+1") in CounterType.KNOWN) shouldBe false
            CounterType.of("oil") shouldBe CounterType("OIL")
        }
    }

    it("lists every known kind once") {
        CounterType.KNOWN.groupBy { it }.filterValues { it.size > 1 }.keys.shouldBeEmpty()
    }

    describe("serialization") {
        val json = Json

        it("encodes the canonical id and decodes any spelling") {
            json.encodeToString(CounterType.serializer(), CounterType.PLUS_ONE_PLUS_ONE) shouldBe "\"PLUS_ONE_PLUS_ONE\""
            json.decodeFromString(CounterType.serializer(), "\"+1/+1\"") shouldBe CounterType.PLUS_ONE_PLUS_ONE
            json.decodeFromString(CounterType.serializer(), "\"first strike\"") shouldBe CounterType.FIRST_STRIKE
        }

        it("decodes the retired CounterTypeFilter shapes") {
            json.decodeFromString(CounterType.serializer(), "\"PlusOnePlusOne\"") shouldBe CounterType.PLUS_ONE_PLUS_ONE
            json.decodeFromString(CounterType.serializer(), "\"MinusZeroMinusOne\"") shouldBe CounterType.MINUS_ZERO_MINUS_ONE
            json.decodeFromString(CounterType.serializer(), "\"Loyalty\"") shouldBe CounterType.LOYALTY
            json.decodeFromString(CounterType.serializer(), """{"type":"Named","name":"stun"}""") shouldBe CounterType.STUN
            json.decodeFromString(CounterType.serializer(), """{"type":"PlusOnePlusZero"}""") shouldBe CounterType.PLUS_ONE_PLUS_ZERO
        }

        it("refuses the retired any-kind wildcard rather than reading it as one kind") {
            shouldThrow<SerializationException> {
                json.decodeFromString(CounterType.serializer(), "\"CounterAny\"")
            }
        }

        it("keeps a counters map keyed by id on the wire") {
            val serializer = MapSerializer(CounterType.serializer(), Int.serializer())
            val counters = mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.FIRST_STRIKE to 1)
            val encoded = json.encodeToString(serializer, counters)
            encoded shouldBe """{"PLUS_ONE_PLUS_ONE":2,"FIRST_STRIKE":1}"""
            json.decodeFromString(serializer, encoded) shouldBe counters
        }

        it("carries an any-kind position as null") {
            val encoded = json.encodeToString(Holder.serializer(), Holder(null))
            json.decodeFromString(Holder.serializer(), encoded) shouldBe Holder(null)
            json.decodeFromString(Holder.serializer(), """{"counterType":"stun"}""") shouldBe Holder(CounterType.STUN)
        }
    }
}) {
    @Serializable
    private data class Holder(val counterType: CounterType?)

    private companion object {
        /** Kinds named after `object Counters` was retired, so they have no legacy constant. */
        val NEW_KINDS = setOf(CounterType.BLOODLINE, CounterType.INVITATION, CounterType.IMPOSTOR, CounterType.DEVOTION,
            CounterType.THEFT, CounterType.TRAINING)

        /** The values of the retired `object Counters` string constants, verbatim. */
        val LEGACY_SPELLINGS: Map<CounterType, String> = mapOf(
            CounterType.PLUS_ONE_PLUS_ONE to "+1/+1", CounterType.MINUS_ONE_MINUS_ONE to "-1/-1",
            CounterType.PLUS_ONE_PLUS_ZERO to "+1/+0", CounterType.PLUS_ZERO_PLUS_ONE to "+0/+1",
            CounterType.PLUS_TWO_PLUS_ZERO to "+2/+0", CounterType.PLUS_ZERO_PLUS_TWO to "+0/+2",
            CounterType.MINUS_ONE_MINUS_ZERO to "-1/-0", CounterType.MINUS_ZERO_MINUS_ONE to "-0/-1",
            CounterType.PLUS_ONE_PLUS_TWO to "+1/+2", CounterType.PLUS_TWO_PLUS_TWO to "+2/+2",
            CounterType.MINUS_TWO_MINUS_TWO to "-2/-2", CounterType.LOYALTY to "loyalty",
            CounterType.DEFENSE to "defense", CounterType.CHARGE to "charge", CounterType.GEM to "gem",
            CounterType.POISON to "poison", CounterType.SILVER to "silver", CounterType.GOLD to "gold",
            CounterType.PLAGUE to "plague", CounterType.TRAP to "trap", CounterType.FATE to "fate",
            CounterType.DEPLETION to "depletion", CounterType.EGG to "egg", CounterType.LORE to "lore",
            CounterType.AIM to "aim", CounterType.STUN to "stun", CounterType.SHIELD to "shield",
            CounterType.FINALITY to "finality", CounterType.SUPPLY to "supply", CounterType.FLYING to "flying",
            CounterType.FIRST_STRIKE to "first strike", CounterType.DOUBLE_STRIKE to "double strike",
            CounterType.VIGILANCE to "vigilance", CounterType.LIFELINK to "lifelink",
            CounterType.INDESTRUCTIBLE to "indestructible", CounterType.DEATHTOUCH to "deathtouch",
            CounterType.TRAMPLE to "trample", CounterType.HEXPROOF to "hexproof", CounterType.REACH to "reach",
            CounterType.HASTE to "haste", CounterType.MENACE to "menace", CounterType.STASH to "stash",
            CounterType.CROAK to "croak", CounterType.BLIGHT to "blight", CounterType.COIN to "coin",
            CounterType.FLOOD to "flood", CounterType.CHORUS to "chorus", CounterType.DREAM to "dream",
            CounterType.QUEST to "quest", CounterType.GROWTH to "growth", CounterType.TIME to "time",
            CounterType.FEATHER to "feather", CounterType.HOURGLASS to "hourglass",
            CounterType.SPORE to "spore", CounterType.DECAYED to "decayed", CounterType.HOPE to "hope",
            CounterType.VERSE to "verse", CounterType.INFLUENCE to "influence", CounterType.BURDEN to "burden",
            CounterType.LOOT to "loot", CounterType.WIND to "wind", CounterType.NEST to "nest",
            CounterType.PAGE to "page", CounterType.HOOFPRINT to "hoofprint",
            CounterType.MANNEQUIN to "mannequin", CounterType.BAIT to "bait", CounterType.REV to "rev",
            CounterType.BLOODSTAIN to "bloodstain", CounterType.BLOOD to "blood", CounterType.SOUL to "soul",
            CounterType.DIVINITY to "divinity", CounterType.OMEN to "omen", CounterType.SUSPECT to "suspect",
            CounterType.DOOM to "doom", CounterType.POSSESSION to "possession", CounterType.FIRE to "fire",
            CounterType.CONQUEROR to "conqueror", CounterType.NET to "net", CounterType.LANDMARK to "landmark",
            CounterType.DREAD to "dread", CounterType.INCUBATION to "incubation",
            CounterType.FELLOWSHIP to "fellowship", CounterType.BORE to "bore", CounterType.POINT to "point",
            CounterType.WISH to "wish", CounterType.REVIVAL to "revival", CounterType.INGENUITY to "ingenuity",
            CounterType.FILM to "film", CounterType.HARNESS to "harness", CounterType.HONE to "hone",
            CounterType.STORAGE to "storage", CounterType.HUNGER to "hunger", CounterType.SLIME to "slime",
            CounterType.JAVELIN to "javelin", CounterType.CREDIT to "credit", CounterType.CUBE to "cube",
            CounterType.TIDE to "tide", CounterType.SKEWER to "skewer", CounterType.ENERGY to "energy",
            CounterType.ICE to "ice", CounterType.PLAN to "plan", CounterType.INVASION to "invasion",
            CounterType.UNLOCK to "unlock", CounterType.JUDGMENT to "judgment"
        )
    }
}
